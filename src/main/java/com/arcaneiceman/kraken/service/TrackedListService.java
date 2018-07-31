package com.arcaneiceman.kraken.service;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.arcaneiceman.kraken.domain.*;
import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.embedded.JobDelimiter;
import com.arcaneiceman.kraken.domain.enumerations.ListType;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.TrackedListRepository;
import com.arcaneiceman.kraken.service.permission.abs.TrackedListPermissionLayer;
import com.arcaneiceman.kraken.util.ConsoleCommandUtil;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import com.ttt.eru.libs.fileupload.configuration.AmazonS3Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Status;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class TrackedListService {

    private TrackedListRepository trackedListRepository;
    private TrackedListPermissionLayer trackedListPermissionLayer;
    private PasswordListService passwordListService;
    private AmazonS3Configuration amazonS3Configuration;
    private JobService jobService;

    // Environment Variables
    @Value("${application.tracked-list-settings.job-size}")
    private String jobSize;
    @Value("${application.tracked-list-settings.password-list.cloud-storage-path}")
    private String passwordListCloudStoragePath;
    @Value("${application.tracked-list-settings.crunch-list.max-total-jobs}")
    private String crunchListMaxTotalJobs;

    public TrackedListService(TrackedListRepository trackedListRepository,
                              TrackedListPermissionLayer trackedListPermissionLayer,
                              PasswordListService passwordListService,
                              AmazonS3Configuration amazonS3Configuration,
                              JobService jobService) {
        this.trackedListRepository = trackedListRepository;
        this.trackedListPermissionLayer = trackedListPermissionLayer;
        this.passwordListService = passwordListService;
        this.amazonS3Configuration = amazonS3Configuration;
        this.jobService = jobService;
    }

    @PostConstruct
    public void checkValues() {
        if (passwordListCloudStoragePath == null || passwordListCloudStoragePath.isEmpty())
            throw new RuntimeException("Application Tracked List Service - " +
                    "Password List Cloud Storage Path Not Specified ");
        if (crunchListMaxTotalJobs == null || crunchListMaxTotalJobs.isEmpty())
            throw new RuntimeException("Application Tracked List Service - " +
                    "Crunch List Max Total Jobs Not Specified");
        if (jobSize == null || jobSize.isEmpty())
            throw new RuntimeException("Application Tracked List Service - " +
                    "Job Size Not Specified");
    }

    public TrackedCrunchList createCrunchList(Integer minSize,
                                              Integer maxSize,
                                              String characters,
                                              String startString,
                                              Request request) {
        // Validate Crunch List
        String response;
        try {
            response = ConsoleCommandUtil.executeCommandInConsole(500, TimeUnit.MILLISECONDS,
                    ConsoleCommandUtil.OutputStream.ERROR,
                    "crunch", minSize.toString(), maxSize.toString(), characters, "-s", startString);
            if (response == null || response.isEmpty())
                throw new RuntimeException("Could not create Crunch Request " + minSize + " " + maxSize + " " + characters);
        } catch (Exception e) {
            throw new SystemException(32423, "Could not create Crunch Request : " + e.getMessage(), Status.BAD_REQUEST);
        }
        Pattern pattern = Pattern.compile("Crunch will now generate the following number of lines: (\\d+)");
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find())
            throw new SystemException(1231, "Could not create Crunch Request "
                    + minSize + " " + maxSize + " " + characters, Status.BAD_REQUEST);
        // Ceil of [Number of lines / jobSize]
        Integer totalJobs = ((Double) Math.ceil(Double.parseDouble(matcher.group(1)) / Double.parseDouble(jobSize))).intValue();
        if (totalJobs > Integer.parseInt(crunchListMaxTotalJobs))
            throw new SystemException(3242, "Number of jobs created by this would exceed " + crunchListMaxTotalJobs, Status.BAD_REQUEST);
        TrackedCrunchList trackedCrunchList = new TrackedCrunchList();
        trackedCrunchList.setStatus(TrackingStatus.PENDING);
        trackedCrunchList.setMinSize(minSize);
        trackedCrunchList.setMaxSize(maxSize);
        trackedCrunchList.setCharacters(characters);
        trackedCrunchList.setNextJobString(startString);
        trackedCrunchList.setJobQueue(new ArrayList<>());
        trackedCrunchList.setTotalJobCount(totalJobs);
        trackedCrunchList.setCompletedJobCount(0);
        trackedCrunchList.setErrorJobCount(0);
        trackedCrunchList.setNextJobIndex(0);
        trackedCrunchList.setListType(ListType.CRUNCH);
        trackedCrunchList.setOwner(request);
        return trackedListRepository.save(trackedCrunchList);
    }

    TrackedPasswordList createPasswordList(String passwordListName, Request request) {
        PasswordList passwordList = passwordListService.get(passwordListName);
        if (passwordList == null)
            throw new SystemException(23423,
                    "Password List with name " + passwordListName + " not found", Status.NOT_FOUND);
        TrackedPasswordList trackedPasswordList = new TrackedPasswordList();
        trackedPasswordList.setPasswordListName(passwordList.getName());
        trackedPasswordList.setStatus(TrackingStatus.PENDING);
        trackedPasswordList.setTotalJobCount(passwordList.getJobDelimiterSet().size());
        trackedPasswordList.setCompletedJobCount(0);
        trackedPasswordList.setErrorJobCount(0);
        trackedPasswordList.setNextJobIndex(0);
        trackedPasswordList.setJobQueue(new ArrayList<>());
        trackedPasswordList.setListType(ListType.PASSWORD_LIST);
        trackedPasswordList.setOwner(request);
        return trackedListRepository.save(trackedPasswordList);
    }

    Job getNextJob(TrackedList trackedList, Worker worker) {
        // Check if there are any jobs in the job queue (which may have timed out or reported as error)
        for (Job job : trackedList.getJobQueue()) {
            if (job.getTrackingStatus() == TrackingStatus.PENDING)
                // Fetch Candidate Values, Mark as RUNNING and return
                try {
                    job.setValues(getCandidateValues(trackedList, job.getStart(), job.getEnd()));
                    job.setTrackingStatus(TrackingStatus.RUNNING);
                    job.setSubmittedAt(new Date());
                    trackedListRepository.save(trackedList);
                    return job;
                }
                // @Fetch Exception, List stops creating jobs and goes into error state
                catch (Exception e) {
                    trackedList.setStatus(TrackingStatus.ERROR);
                    trackedListRepository.save(trackedList);
                    return null;
                }
        }

        // Get Next Job From List
        int nextJobIndex = trackedList.getNextJobIndex();
        if (nextJobIndex < trackedList.getTotalJobCount()) {
            // Increment NextJobIndex for next time
            trackedList.setNextJobIndex(trackedList.getNextJobIndex() + 1);
            // Fetch Candidate Values, Add to Job Queue and return
            try {


                String start = null;
                String end = null;
                List<String> candidateValues = new ArrayList<>();
                switch (trackedList.getListType()) {
                    case PASSWORD_LIST:
                        TrackedPasswordList trackedPasswordList = (TrackedPasswordList) trackedList;
                        // Get Job Delimiter
                        JobDelimiter jobDelimiter = passwordListService.getJobDelimiterForPasswordList(
                                trackedPasswordList.getPasswordListName(), nextJobIndex);
                        // Mark Start and End
                        start = Long.toString(jobDelimiter.getStartByte());
                        end = Long.toString(jobDelimiter.getEndByte());
                        // Fetch Values
                        candidateValues = getCandidateValues(trackedList, start, end);
                        break;
                    case CRUNCH:
                        TrackedCrunchList trackedCrunchList = (TrackedCrunchList) trackedList;
                        // Get Next Job String
                        start = trackedCrunchList.getNextJobString();
                        // Generate Values
                        candidateValues = getCandidateValues(trackedList, start, null);
                        // Set End String
                        end = candidateValues.get(candidateValues.size() - 1);
                        // Update Next Job String
                        trackedCrunchList.setNextJobString(end);
                        break;
                }

                // Create Job and add to job queue
                Job job = jobService.create(nextJobIndex, start, end, worker, trackedList);
                worker.setJob(job);
                trackedList.getJobQueue().add(job);
                if (trackedList.getStatus() != TrackingStatus.RUNNING)
                    trackedList.setStatus(TrackingStatus.RUNNING);

                // Save
                trackedListRepository.save(trackedList);
                // Inject Transient Values
                job.setValues(candidateValues);
                return job;
            }
            // @Fetch Exception, List stops creating jobs and goes into error state
            catch (Exception e) {
                trackedList.setStatus(TrackingStatus.ERROR);
                trackedListRepository.save(trackedList);
                return null;
            }
        }

        // There are no more jobs left to create. Check if list is complete. If it is, mark it as complete.
        if (checkIfListComplete(trackedList))
            trackedList.setStatus(TrackingStatus.COMPLETE);
        trackedListRepository.save(trackedList);
        return null;

    }

    private List<String> getCandidateValues(TrackedList trackedList, String start, String end) throws Exception {
        List<String> candidateValues = new ArrayList<>();
        switch (trackedList.getListType()) {
            case PASSWORD_LIST:
                TrackedPasswordList trackedPasswordList = (TrackedPasswordList) trackedList;
                PasswordList passwordList = passwordListService.getOrNull(trackedPasswordList.getPasswordListName());
                if (passwordList == null)
                    throw new Exception("Password List Missing");
                GetObjectRequest getObjectRequest = new GetObjectRequest(amazonS3Configuration.getAmazonS3BucketName(),
                        passwordListCloudStoragePath + "/" + passwordList.getName());
                getObjectRequest.setRange(Long.parseLong(start), Long.parseLong(end));
                S3Object object = amazonS3Configuration.generateClient().getObject(getObjectRequest);
                InputStream fileStream = new BufferedInputStream(object.getObjectContent());
                InputStreamReader decoder = new InputStreamReader(fileStream, passwordList.getCharset());
                BufferedReader buffered = new BufferedReader(decoder);
                String thisLine;
                while ((thisLine = buffered.readLine()) != null)
                    candidateValues.add(thisLine);
                break;
            case CRUNCH:
                TrackedCrunchList trackedCrunchList = (TrackedCrunchList) trackedList;
                String response = ConsoleCommandUtil.executeCommandInConsole(5, TimeUnit.SECONDS,
                        ConsoleCommandUtil.OutputStream.OUT,
                        "crunch",
                        trackedCrunchList.getMinSize().toString(), trackedCrunchList.getMaxSize().toString(),
                        trackedCrunchList.getCharacters(), "-s", start, "-c", jobSize);
                if (response == null || response.isEmpty())
                    throw new Exception("Could not create Crunch Request " +
                            trackedCrunchList.getMinSize().toString() + " " + trackedCrunchList.getMaxSize().toString() +
                            " " + trackedCrunchList.getCharacters());
                candidateValues.addAll(Arrays.asList(response.split("\n")));
                break;
        }
        return candidateValues;
    }

    void reportJob(String jobId, Long trackedListId, Request request, TrackingStatus trackingStatus, Worker worker) {
        TrackedList trackedList = trackedListPermissionLayer.getWithOwner(trackedListId, request);
        Job job = jobService.get(jobId, trackedList);

        // Remove association between job and worker
        if (!worker.getJob().equals(job))
            throw new SystemException(2342, "This worker was not running reported job", Status.BAD_REQUEST);
        else {
            worker.setJob(null);
            job.setWorker(null);
        }

        // Based on tracking status
        switch (trackingStatus) {
            case COMPLETE:
                // Increment Complete Count
                trackedList.setCompletedJobCount(trackedList.getCompletedJobCount() + 1);
                // Remove from Queue
                trackedList.getJobQueue().remove(job);
                break;
            case ERROR:
                // Increment Error Count
                job.setErrorCount(job.getErrorCount() + 1);
                // If Error Count is above 3, remove it from queue and add it to error count
                if (job.getErrorCount() > 3) {
                    trackedList.getJobQueue().remove(job);
                    trackedList.setErrorJobCount(trackedList.getErrorJobCount() + 1);
                }
                // Else, make it pending
                else
                    job.setTrackingStatus(TrackingStatus.PENDING);
                break;
            default:
                throw new SystemException(423, "Job Report can either be COMPLETE or ERROR", Status.BAD_REQUEST);
        }

        if (checkIfListComplete(trackedList))
            trackedList.setStatus(TrackingStatus.COMPLETE);

        trackedListRepository.save(trackedList);
    }

    private boolean checkIfListComplete(TrackedList TrackedList) {
        // If Complete Jobs + Error Jobs == Total Jobs, this list is complete
        int reportedJobCount = TrackedList.getCompletedJobCount() + TrackedList.getErrorJobCount();
        return Objects.equals(reportedJobCount, TrackedList.getTotalJobCount());
    }

    // TODO : Make this a Quartz Job
    @Scheduled(initialDelay = 5000L, fixedDelayString = "${application.tracked-list-settings.job-expiry-task-delay-in-milliseconds}")
    public void retireJob() {
        List<Job> expiredJobs = jobService.getExpiredJobs();
        for (Job job : expiredJobs) {
            reportJob(job.getId(), job.getOwner().getId(), job.getOwner().getOwner(), TrackingStatus.ERROR, job.getWorker());
        }
    }

}

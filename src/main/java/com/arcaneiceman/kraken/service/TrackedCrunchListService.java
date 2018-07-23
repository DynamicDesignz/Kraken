package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.TrackedCrunchList;
import com.arcaneiceman.kraken.domain.embedded.Job;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.TrackedCrunchListRepository;
import com.arcaneiceman.kraken.service.permission.abs.TrackedCrunchListPermissionLayer;
import com.arcaneiceman.kraken.util.ConsoleCommandUtil;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Status;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class TrackedCrunchListService {

    private TrackedCrunchListRepository trackedCrunchListRepository;
    private TrackedCrunchListPermissionLayer trackedCrunchListPermissionLayer;

    @Value("${application.password-list-settings.job-size}")
    private String jobSize;

    public TrackedCrunchListService(TrackedCrunchListRepository trackedCrunchListRepository,
                                    TrackedCrunchListPermissionLayer trackedCrunchListPermissionLayer) {
        this.trackedCrunchListRepository = trackedCrunchListRepository;
        this.trackedCrunchListPermissionLayer = trackedCrunchListPermissionLayer;
    }

    @PostConstruct
    public void checkValues() {
        if (jobSize == null || jobSize.isEmpty())
            throw new RuntimeException("Application Candidate Value List Updater : Job Size Not Specified");
    }

    public TrackedCrunchList create(Integer minSize,
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
        TrackedCrunchList trackedCrunchList = new TrackedCrunchList(null,
                minSize,
                maxSize,
                characters,
                TrackingStatus.PENDING,
                totalJobs,
                startString,
                0,
                0,
                0,
                new ArrayList<>());
        trackedCrunchList.setOwner(request);
        return trackedCrunchListRepository.save(trackedCrunchList);
    }

    public Job getNextJob(TrackedCrunchList trackedCrunchList) {
        // Check if there are any jobs in the job queue (which may have timed out or reported as error)
        for (Job job : trackedCrunchList.getJobQueue()) {
            if (job.getTrackingStatus() == TrackingStatus.PENDING)
                // Fetch Candidate Values, Mark as RUNNING and return
                try {
                    job.setValues(getCandidateValues(
                            trackedCrunchList.getMinSize().toString(),
                            trackedCrunchList.getMaxSize().toString(),
                            trackedCrunchList.getCharacters(), job.getStart()));
                    job.setTrackingStatus(TrackingStatus.RUNNING);
                    trackedCrunchListRepository.save(trackedCrunchList);
                    return job;
                }
                // Fetch Exception, remove job, increment error count and check if it is complete (with errors)
                catch (Exception e) {
                    trackedCrunchList.setErrorJobCount(trackedCrunchList.getErrorJobCount() + 1);
                    trackedCrunchList.getJobQueue().remove(job);
                    if (checkIfListComplete(trackedCrunchList))
                        trackedCrunchList.setStatus(TrackingStatus.COMPLETE);
                    trackedCrunchListRepository.save(trackedCrunchList);
                }
        }

        // Get Next Job From Crunch
        int nextJobIndex = trackedCrunchList.getNextJobIndex();
        if (trackedCrunchList.getNextJobIndex() >= trackedCrunchList.getTotalJobs()) {
            // Increment Next Index For Next Run
            trackedCrunchList.setNextJobIndex(trackedCrunchList.getNextJobIndex() + 1);
            // Fetch Candidate Values, Add to Job Queue and return
            try {
                // Create Timeout Time
                Date timeOutDate = new Date();
                timeOutDate.setTime(new Date().getTime() + 1000000L);

                // Fetch Candidate Values
                String startString = trackedCrunchList.getNextJobString();
                List<String> candidateValues = getCandidateValues(
                        trackedCrunchList.getMinSize().toString(),
                        trackedCrunchList.getMaxSize().toString(),
                        trackedCrunchList.getCharacters(), startString);
                String endString = candidateValues.get(candidateValues.size() - 1);

                // Create Job and add to job queue
                Job job = new Job(
                        nextJobIndex,
                        TrackingStatus.RUNNING,
                        startString,
                        endString,
                        0,
                        timeOutDate,
                        candidateValues);
                trackedCrunchList.getJobQueue().add(job);
                if (trackedCrunchList.getStatus() != TrackingStatus.RUNNING)
                    trackedCrunchList.setStatus(TrackingStatus.RUNNING);

                // Update Next Job String
                trackedCrunchList.setNextJobString(endString);

                // Save and return Job
                trackedCrunchListRepository.save(trackedCrunchList);
                return job;
            }
            // Fetch Exception. Crunch last cannot create the next job without the previous one. Set as ERROR
            catch (Exception e) {
                trackedCrunchList.setStatus(TrackingStatus.ERROR);
                trackedCrunchListRepository.save(trackedCrunchList);
                return null;
            }
        }

        // There are no more jobs left to create. Check if list is complete. If it is, mark it as complete.
        if (checkIfListComplete(trackedCrunchList))
            trackedCrunchList.setStatus(TrackingStatus.COMPLETE);
        trackedCrunchListRepository.save(trackedCrunchList);
        return null;
    }

    void reportJob(Long id, Integer jobIndexNumber, TrackingStatus trackingStatus, Request request) {
        TrackedCrunchList trackedCrunchList = trackedCrunchListPermissionLayer.getWithOwner(id, request);

        // Find Running Job with index number
        Job job = trackedCrunchList.getJobQueue().stream()
                .filter(jobFromStream -> Objects.equals(jobFromStream.getIndexNumber(), jobIndexNumber)
                        && jobFromStream.getTrackingStatus() == TrackingStatus.RUNNING).findFirst()
                .orElseThrow(() -> new SystemException(2131, "Could not find running job with indexNumber " + jobIndexNumber, Status.NOT_FOUND));

        // Based on tracking status
        switch (trackingStatus) {
            case COMPLETE:
                // Increment Complete Count
                trackedCrunchList.setCompletedJobCount(trackedCrunchList.getCompletedJobCount() + 1);
                // Remove from Queue
                trackedCrunchList.getJobQueue().remove(job);
                break;
            case ERROR:
                // Increment Error Count
                job.setErrorCount(job.getErrorCount() + 1);
                // If Error Count is above 3, remove it from queue and add it to error count
                if (job.getErrorCount() > 3) {
                    trackedCrunchList.getJobQueue().remove(job);
                    trackedCrunchList.setErrorJobCount(trackedCrunchList.getErrorJobCount() + 1);
                }
                // Else, make it pending
                else
                    job.setTrackingStatus(TrackingStatus.PENDING);
                break;
            default:
                throw new SystemException(423, "Job Report can either be COMPLETE or ERROR", Status.BAD_REQUEST);
        }

        if (checkIfListComplete(trackedCrunchList))
            trackedCrunchList.setStatus(TrackingStatus.COMPLETE);

        trackedCrunchListRepository.save(trackedCrunchList);
    }


    private List<String> getCandidateValues(String minSize, String maxSize, String characters, String startString)
            throws InterruptedException, ExecutionException, IOException {
        String response = ConsoleCommandUtil.executeCommandInConsole(5, TimeUnit.SECONDS,
                ConsoleCommandUtil.OutputStream.OUT,
                "crunch", minSize, maxSize, characters, "-s", startString, "-c", jobSize);
        if (response == null || response.isEmpty())
            throw new RuntimeException("Could not create Crunch Request " + minSize + " " + maxSize + " " + characters);
        return Arrays.asList(response.split("\n"));
    }

    private boolean checkIfListComplete(TrackedCrunchList trackedCrunchList) {
        // If Complete Jobs + Error Jobs == Total Jobs, this list is complete
        int reportedJobCount = trackedCrunchList.getCompletedJobCount() + trackedCrunchList.getErrorJobCount();
        return Objects.equals(reportedJobCount, trackedCrunchList.getTotalJobs());
    }
}

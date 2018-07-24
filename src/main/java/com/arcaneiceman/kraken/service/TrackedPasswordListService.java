package com.arcaneiceman.kraken.service;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.arcaneiceman.kraken.domain.PasswordList;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import com.arcaneiceman.kraken.domain.embedded.Job;
import com.arcaneiceman.kraken.domain.embedded.JobDelimiter;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.TrackedPasswordListRepository;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import com.ttt.eru.libs.fileupload.configuration.AmazonS3Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Status;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class TrackedPasswordListService {

    private TrackedPasswordListRepository trackedPasswordListRepository;
    private AmazonS3Configuration amazonS3Configuration;
    private PasswordListService passwordListService;

    @Value("${application.password-list-settings.folder-prefix}")
    private String candidateValueListStoragePath;

    public TrackedPasswordListService(TrackedPasswordListRepository trackedPasswordListRepository,
                                      AmazonS3Configuration amazonS3Configuration,
                                      PasswordListService passwordListService) {
        this.trackedPasswordListRepository = trackedPasswordListRepository;
        this.amazonS3Configuration = amazonS3Configuration;
        this.passwordListService = passwordListService;
    }

    @PostConstruct
    public void checkValues() {
        if (candidateValueListStoragePath == null || candidateValueListStoragePath.isEmpty())
            throw new RuntimeException("Application Tracked Password List Service - " +
                    "Password List S3 Path : Storage Path Not Specified ");
    }

    TrackedPasswordList create(String passwordListName, Request request) {
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
        trackedPasswordList.setOwner(request);
        return trackedPasswordListRepository.save(trackedPasswordList);
    }

    Job getNextJob(TrackedPasswordList trackedPasswordList) {
        // Attempt to fetch password list. If not found, mark tracked password list as error and return null
        PasswordList passwordList = passwordListService.getOrNull(trackedPasswordList.getPasswordListName());
        if (passwordList == null) {
            trackedPasswordList.setStatus(TrackingStatus.ERROR);
            trackedPasswordListRepository.save(trackedPasswordList);
            return null;
        }

        // Check if there are any jobs in the job queue (which may have timed out or reported as error)
        for (Job job : trackedPasswordList.getJobQueue()) {
            if (job.getTrackingStatus() == TrackingStatus.PENDING)
                // Fetch Candidate Values, Mark as RUNNING and return
                try {
                    job.setValues(getCandidateValues(Long.parseLong(job.getStart()), Long.parseLong(job.getEnd()), passwordList));
                    job.setTrackingStatus(TrackingStatus.RUNNING);
                    trackedPasswordListRepository.save(trackedPasswordList);
                    return job;
                }
                // Fetch Exception, remove job, increment error count and check if it is complete (with errors)
                catch (Exception e) {
                    trackedPasswordList.setErrorJobCount(trackedPasswordList.getErrorJobCount() + 1);
                    trackedPasswordList.getJobQueue().remove(job);
                    if (checkIfListComplete(trackedPasswordList))
                        trackedPasswordList.setStatus(TrackingStatus.COMPLETE);
                    trackedPasswordListRepository.save(trackedPasswordList);
                }
        }

        // Get Next Job From List
        int nextJobIndex = trackedPasswordList.getNextJobIndex();
        while (nextJobIndex < trackedPasswordList.getTotalJobCount()) {
            JobDelimiter jobDelimiter = passwordList.getJobDelimiterSet().get(trackedPasswordList.getNextJobIndex());
            // Increment NextJobIndex for next time
            trackedPasswordList.setNextJobIndex(trackedPasswordList.getNextJobIndex() + 1);
            // Fetch Candidate Values, Add to Job Queue and return
            try {
                // Create Timeout Time
                Date timeOutDate = new Date();
                timeOutDate.setTime(new Date().getTime() + 1000000L);

                // Create Job and add to job queue
                Job job = new Job(
                        nextJobIndex,
                        TrackingStatus.RUNNING,
                        Long.toString(jobDelimiter.getStartByte()),
                        Long.toString(jobDelimiter.getEndByte()),
                        0,
                        timeOutDate,
                        getCandidateValues(jobDelimiter.getStartByte(), jobDelimiter.getEndByte(), passwordList));
                trackedPasswordList.getJobQueue().add(job);
                if (trackedPasswordList.getStatus() != TrackingStatus.RUNNING)
                    trackedPasswordList.setStatus(TrackingStatus.RUNNING);

                // Save and return Job
                trackedPasswordListRepository.save(trackedPasswordList);
                return job;
            }
            // @Fetch Exception, Increment error
            catch (Exception e) {
                trackedPasswordList.setErrorJobCount(trackedPasswordList.getErrorJobCount() + 1);
            }
        }

        // There are no more jobs left to create. Check if list is complete. If it is, mark it as complete.
        if (checkIfListComplete(trackedPasswordList))
            trackedPasswordList.setStatus(TrackingStatus.COMPLETE);
        trackedPasswordListRepository.save(trackedPasswordList);
        return null;
    }

    void reportJob(TrackedPasswordList trackedPasswordList, Integer jobIndexNumber, TrackingStatus trackingStatus) {
        // Find Running Job with index number
        Job job = trackedPasswordList.getJobQueue().stream()
                .filter(jobFromStream -> Objects.equals(jobFromStream.getIndexNumber(), jobIndexNumber)
                        && jobFromStream.getTrackingStatus() == TrackingStatus.RUNNING).findFirst()
                .orElseThrow(() -> new SystemException(2131, "Could not find running job with indexNumber " + jobIndexNumber, Status.NOT_FOUND));

        // Based on tracking status
        switch (trackingStatus) {
            case COMPLETE:
                // Increment Complete Count
                trackedPasswordList.setCompletedJobCount(trackedPasswordList.getCompletedJobCount() + 1);
                // Remove from Queue
                trackedPasswordList.getJobQueue().remove(job);
                break;
            case ERROR:
                // Increment Error Count
                job.setErrorCount(job.getErrorCount() + 1);
                // If Error Count is above 3, remove it from queue and add it to error count
                if (job.getErrorCount() > 3) {
                    trackedPasswordList.getJobQueue().remove(job);
                    trackedPasswordList.setErrorJobCount(trackedPasswordList.getErrorJobCount() + 1);
                }
                // Else, make it pending
                else
                    job.setTrackingStatus(TrackingStatus.PENDING);
                break;
            default:
                throw new SystemException(423, "Job Report can either be COMPLETE or ERROR", Status.BAD_REQUEST);
        }

        if (checkIfListComplete(trackedPasswordList))
            trackedPasswordList.setStatus(TrackingStatus.COMPLETE);

        trackedPasswordListRepository.save(trackedPasswordList);
    }

    private List<String> getCandidateValues(Long startByte, Long endByte, PasswordList passwordList)
            throws IOException {
        List<String> candidateValues = new ArrayList<>();
        GetObjectRequest getObjectRequest = new GetObjectRequest(amazonS3Configuration.getAmazonS3BucketName(),
                candidateValueListStoragePath + "/" + passwordList.getName());
        getObjectRequest.setRange(startByte, endByte);
        S3Object object = amazonS3Configuration.generateClient().getObject(getObjectRequest);
        InputStream fileStream = new BufferedInputStream(object.getObjectContent());
        InputStreamReader decoder = new InputStreamReader(fileStream, passwordList.getCharset());
        BufferedReader buffered = new BufferedReader(decoder);
        String thisLine;
        while ((thisLine = buffered.readLine()) != null)
            candidateValues.add(thisLine);
        return candidateValues;
    }

    private boolean checkIfListComplete(TrackedPasswordList trackedPasswordList) {
        // If Complete Jobs + Error Jobs == Total Jobs, this list is complete
        int reportedJobCount = trackedPasswordList.getCompletedJobCount() + trackedPasswordList.getErrorJobCount();
        return Objects.equals(reportedJobCount, trackedPasswordList.getTotalJobCount());
    }
}

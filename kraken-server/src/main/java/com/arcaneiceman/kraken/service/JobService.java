package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.Job;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.JobRepository;
import com.arcaneiceman.kraken.service.permission.abs.JobPermissionLayer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class JobService {

    private JobRepository jobRepository;
    private JobPermissionLayer jobPermissionLayer;

    @Value("${application.tracked-list-settings.job-expiry-in-milliseconds}")
    private String jobExpiry;

    public JobService(JobRepository jobRepository, JobPermissionLayer jobPermissionLayer) {
        this.jobRepository = jobRepository;
        this.jobPermissionLayer = jobPermissionLayer;
    }

    @PostConstruct
    public void checkVariables() {
        if (jobExpiry == null || jobExpiry.isEmpty())
            throw new RuntimeException("Application Job Service - Job Expiry Time Not Specified");
    }

    Job create(Integer indexNumber, String start, String end, Worker worker, TrackedList trackedList) {
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setIndexNumber(indexNumber);
        job.setTrackingStatus(TrackingStatus.RUNNING);
        job.setStart(start);
        job.setEnd(end);
        job.setErrorCount(0);
        job.setSubmittedAt(new Date());
        job.setWorker(worker);
        job.setOwner(trackedList);
        return jobRepository.save(job);
    }

    Job get(String id, TrackedList trackedList) {
        return jobPermissionLayer.getWithOwner(id, trackedList);
    }

    List<Job> getExpiredJobs() {
        // submittedAt < currentTime - expiryTime
        return jobRepository.findByTrackingStatusAndSubmittedAtBefore(TrackingStatus.RUNNING,
                new Date(new Date().getTime() - Long.parseLong(jobExpiry)));
    }

}

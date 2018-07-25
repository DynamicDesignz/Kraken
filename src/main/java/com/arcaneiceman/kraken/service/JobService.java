package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.Job;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.JobRepository;
import com.arcaneiceman.kraken.service.permission.abs.JobPermissionLayer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
@Transactional
public class JobService {

    private JobRepository jobRepository;
    private JobPermissionLayer jobPermissionLayer;

    public JobService(JobRepository jobRepository, JobPermissionLayer jobPermissionLayer) {
        this.jobRepository = jobRepository;
        this.jobPermissionLayer = jobPermissionLayer;
    }

    Job create(Integer indexNumber, String start, String end, Worker worker, TrackedList trackedList) {
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setIndexNumber(indexNumber);
        job.setTrackingStatus(TrackingStatus.RUNNING);
        job.setStart(start);
        job.setEnd(end);
        job.setErrorCount(0);

        // Create Timeout Time
        Date timeOutDate = new Date();
        timeOutDate.setTime(new Date().getTime() + 1000000L);
        job.setTimeoutAt(timeOutDate);

        job.setWorker(worker);
        job.setOwner(trackedList);

        return jobRepository.save(job);
    }

    Job get(String id, TrackedList trackedList) {
        return jobPermissionLayer.getWithOwner(id, trackedList);
    }

}

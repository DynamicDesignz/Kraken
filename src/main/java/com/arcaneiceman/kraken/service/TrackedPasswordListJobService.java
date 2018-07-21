package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import com.arcaneiceman.kraken.domain.TrackedPasswordListJob;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.TrackedPasswordListJobRepository;
import com.arcaneiceman.kraken.service.permission.abs.TrackedPasswordListJobPermissionLayer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TrackedPasswordListJobService {

    private TrackedPasswordListJobRepository trackedPasswordListJobRepository;
    private TrackedPasswordListJobPermissionLayer trackedPasswordListJobPermissionLayer;

    public TrackedPasswordListJobService(TrackedPasswordListJobRepository trackedPasswordListJobRepository, TrackedPasswordListJobPermissionLayer trackedPasswordListJobPermissionLayer) {
        this.trackedPasswordListJobRepository = trackedPasswordListJobRepository;
        this.trackedPasswordListJobPermissionLayer = trackedPasswordListJobPermissionLayer;
    }

    public TrackedPasswordListJob create(Long startByte, Long endByte, TrackedPasswordList trackedPasswordList) {
        TrackedPasswordListJob trackedPasswordListJob = new TrackedPasswordListJob(
                UUID.randomUUID().toString(),
                startByte,
                endByte,
                TrackingStatus.PENDING);
        trackedPasswordListJob.setOwner(trackedPasswordList);
        return trackedPasswordListJobRepository.save(trackedPasswordListJob);
    }

    public TrackedPasswordListJob getNextTrackedJob(TrackedPasswordList owner) {
        return trackedPasswordListJobRepository.findFirstByOwnerAndStatus(owner, TrackingStatus.PENDING);
    }

    public TrackedPasswordListJob markJobAsRunning(TrackedPasswordListJob trackedJob) {
        trackedJob.setStatus(TrackingStatus.RUNNING);
        return trackedPasswordListJobRepository.save(trackedJob);
    }

    public TrackedPasswordListJob markJobAsError(TrackedPasswordListJob trackedJob) {
        trackedJob.setStatus(TrackingStatus.ERROR);
        return trackedPasswordListJobRepository.save(trackedJob);
    }

    public TrackedPasswordListJob markJobAsComplete(TrackedPasswordListJob trackedJob) {
        trackedJob.setStatus(TrackingStatus.COMPLETE);
        return trackedPasswordListJobRepository.save(trackedJob);
    }
}

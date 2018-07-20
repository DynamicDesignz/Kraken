package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.TrackedJob;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.TrackedJobRepository;
import com.arcaneiceman.kraken.service.permission.abs.TrackedJobPermissionLayer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrackedPasswordListJobService {

    private TrackedJobRepository trackedJobRepository;
    private TrackedJobPermissionLayer trackedJobPermissionLayer;

    public TrackedPasswordListJobService(TrackedJobRepository trackedJobRepository, TrackedJobPermissionLayer trackedJobPermissionLayer) {
        this.trackedJobRepository = trackedJobRepository;
        this.trackedJobPermissionLayer = trackedJobPermissionLayer;
    }

    public TrackedJob getNextTrackedJobForRequest(Request request) {
        return trackedJobRepository.findFirstByOwnerAndStatus(request, TrackingStatus.PENDING);
    }

    public TrackedJob getTrackedJob(Request owner, String id) {
        return trackedJobPermissionLayer.getWithOwner(id, owner);
    }

    public TrackedJob markJobAsRunning(TrackedJob trackedJob) {
        trackedJob.setStatus(TrackingStatus.RUNNING);
        return trackedJobRepository.save(trackedJob);
    }

    public TrackedJob markJobAsError(TrackedJob trackedJob) {
        trackedJob.setStatus(TrackingStatus.ERROR);
        return trackedJobRepository.save(trackedJob);
    }

    public TrackedJob markJobAsComplete(TrackedJob trackedJob){
        trackedJob.setStatus(TrackingStatus.COMPLETE);
        return trackedJobRepository.save(trackedJob);
    }
}

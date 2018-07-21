package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.PasswordList;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import com.arcaneiceman.kraken.domain.TrackedPasswordListJob;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.TrackedPasswordListRepository;
import com.arcaneiceman.kraken.service.permission.abs.TrackedPasswordListPermissionLayer;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Status;

import java.util.HashSet;

@Service
@Transactional
public class TrackedPasswordListService {

    private TrackedPasswordListRepository trackedPasswordListRepository;
    private TrackedPasswordListPermissionLayer trackedPasswordListPermissionLayer;
    private TrackedPasswordListJobService trackedPasswordListJobService;
    private PasswordListService passwordListService;


    public TrackedPasswordListService(TrackedPasswordListRepository trackedPasswordListRepository,
                                      TrackedPasswordListPermissionLayer trackedPasswordListPermissionLayer,
                                      TrackedPasswordListJobService trackedPasswordListJobService,
                                      PasswordListService passwordListService) {
        this.trackedPasswordListRepository = trackedPasswordListRepository;
        this.trackedPasswordListPermissionLayer = trackedPasswordListPermissionLayer;
        this.trackedPasswordListJobService = trackedPasswordListJobService;
        this.passwordListService = passwordListService;
    }

    TrackedPasswordList create(String passwordListName, Request request) {
        PasswordList passwordList = passwordListService.get(passwordListName);
        if (passwordList == null)
            throw new SystemException(23423,
                    "Candidate Value List with name " + passwordListName + " not found", Status.NOT_FOUND);
        TrackedPasswordList trackedPasswordList = new TrackedPasswordList(null,
                passwordList.getName(),
                passwordList.getCharset(),
                TrackingStatus.PENDING, null);
        trackedPasswordList.setOwner(request);
        return trackedPasswordListRepository.save(trackedPasswordList);
    }

    TrackedPasswordListJob getNextJob(Long id, Request request) {
        TrackedPasswordList trackedPasswordList = trackedPasswordListPermissionLayer.getWithOwner(id, request);
        switch (trackedPasswordList.getStatus()) {
            case PENDING:
                // Create Tracked Jobs out of the PasswordList
                PasswordList passwordList = passwordListService.get(trackedPasswordList.getPasswordListName());
                if (passwordList == null) {
                    trackedPasswordList.setStatus(TrackingStatus.ERROR);
                    trackedPasswordListRepository.save(trackedPasswordList);
                    return null;
                }
                trackedPasswordList.setTrackedPasswordListJobs(new HashSet<>());
                passwordList.getJobDelimiterSet().forEach(jobDelimiter ->
                        trackedPasswordList.getTrackedPasswordListJobs().add(trackedPasswordListJobService.create(
                                jobDelimiter.getStartByte(),
                                jobDelimiter.getEndByte(),
                                trackedPasswordList)));
                break;
            case RUNNING:
                // This list is already running so it does not need to be prepared
                break;
            case COMPLETE:
                throw new SystemException(234, "Tracked Password List is complete already!", Status.INTERNAL_SERVER_ERROR);
            case ERROR:
                throw new SystemException(234, "Tracked Password List is in error state", Status.INTERNAL_SERVER_ERROR);
        }

        // Fetch Next Job (Could return null if no job available)
        TrackedPasswordListJob trackedPasswordListJob = trackedPasswordListJobService.getNextTrackedJob(trackedPasswordList);
        if (trackedPasswordListJob != null)
            return trackedPasswordListJobService.markJobAsRunning(trackedPasswordListJob);
        else
            return null;
    }
}

package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.TrackedCrunchList;
import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.embedded.Job;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import com.arcaneiceman.kraken.domain.request.detail.WPARequestDetail;
import com.arcaneiceman.kraken.repository.RequestRepository;
import com.arcaneiceman.kraken.service.permission.abs.RequestPermissionLayer;
import com.arcaneiceman.kraken.service.request.detail.MatchRequestDetailService;
import com.arcaneiceman.kraken.service.request.detail.WPARequestDetailService;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import com.ttt.eru.libs.fileupload.configuration.AmazonS3Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.FOUND;
import static org.zalando.problem.Status.NO_CONTENT;

@Service
@Transactional
public class RequestService {

    @Value("${application.kraken-request-settings.folder-prefix}")
    private String passwordCaptureStoragePath;

    @Value("${application.candidate-value-list-settings.folder-prefix}")
    private String candidateValueListStoragePath;

    private UserService userService;
    private AmazonS3Configuration amazonS3Configuration;
    private PasswordListService passwordListService;
    private RequestPermissionLayer requestPermissionLayer;
    private RequestRepository requestRepository;
    private WPARequestDetailService wpaRequestDetailService;
    private MatchRequestDetailService matchRequestDetailService;
    private TrackedPasswordListService trackedPasswordListService;
    private TrackedCrunchListService trackedCrunchListService;

    public RequestService(UserService userService,
                          AmazonS3Configuration amazonS3Configuration,
                          PasswordListService passwordListService,
                          RequestPermissionLayer requestPermissionLayer,
                          RequestRepository requestRepository,
                          WPARequestDetailService wpaRequestDetailService,
                          MatchRequestDetailService matchRequestDetailService,
                          TrackedPasswordListService trackedPasswordListService,
                          TrackedCrunchListService trackedCrunchListService) {
        this.userService = userService;
        this.amazonS3Configuration = amazonS3Configuration;
        this.passwordListService = passwordListService;
        this.requestPermissionLayer = requestPermissionLayer;
        this.requestRepository = requestRepository;
        this.wpaRequestDetailService = wpaRequestDetailService;
        this.matchRequestDetailService = matchRequestDetailService;
        this.trackedPasswordListService = trackedPasswordListService;
        this.trackedCrunchListService = trackedCrunchListService;
    }

    @PostConstruct
    public void checkValues() {
        if (candidateValueListStoragePath == null || candidateValueListStoragePath.isEmpty())
            throw new RuntimeException("Application Request Service - Candidate Value List S3 Path : " +
                    "Storage Path Not Specified ");
        if (passwordCaptureStoragePath == null || passwordCaptureStoragePath.isEmpty())
            throw new RuntimeException("Application Request Service - Capture Fetcher : " +
                    "Storage Path Not Specified");
    }

    public Request createRequest(RequestIO.Create.Request requestDTO,
                                 MultipartFile passwordCaptureFile) {
        User user = userService.getUserOrThrow();
        // Init Request
        Request request = requestRepository.save(new Request());
        request.setOwner(user);

        // Set Request Detail
        switch (requestDTO.getRequestType()) {
            case WPA:
                request.setRequestDetail(
                        wpaRequestDetailService.create((WPARequestDetail) requestDTO.getRequestDetail(), passwordCaptureFile));
            case MATCH:
                request.setRequestDetail(
                        matchRequestDetailService.create((MatchRequestDetail) requestDTO));
        }

        // Create Tracked Password List
        request.setTrackedPasswordLists(new ArrayList<>());
        if (requestDTO.getPasswordLists() != null && !request.getTrackedPasswordLists().isEmpty())
            requestDTO.getPasswordLists().forEach(passwordListName ->
                    request.getTrackedPasswordLists().add(trackedPasswordListService.create(passwordListName, request)));

        // Create Tracked Crunch Lists
        request.setTrackedCrunchLists(new ArrayList<>());
        if (requestDTO.getCrunchParams() != null && !requestDTO.getCrunchParams().isEmpty())
            requestDTO.getCrunchParams().forEach(crunchParams ->
                    request.getTrackedCrunchLists().add(trackedCrunchListService.create(
                            crunchParams.getMinSize(),
                            crunchParams.getMaxSize(),
                            crunchParams.getCharacters(),
                            crunchParams.getStartString(),
                            request)));

        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Request get(Long id) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(id, user);
        request.getTrackedPasswordLists().size();
        request.getTrackedCrunchLists().size();
        return request;
    }

    public RequestIO.GetJob.Response getJob() {
        User user = userService.getUserOrThrow();
        // Get All Requests
        List<Request> requestList = requestRepository.findByOwner(user);

        for (Request request : requestList) {
            // First preference is given to trackedPasswordLists
            // Dispatch next available job in trackedPasswordLists
            for (TrackedPasswordList trackedPasswordList : request.getTrackedPasswordLists()) {
                if (trackedPasswordList.getStatus() == TrackingStatus.PENDING) {
                    Job job = trackedPasswordListService.getNextJob(trackedPasswordList);
                    if (job != null)
                        return new RequestIO.GetJob.Response(
                                request.getRequestType(),
                                getRequestDetail(request),
                                trackedPasswordList.getId(),
                                job.getIndexNumber(),
                                job.getValues());
                }
            }

            // No jobs to dispatch in trackedPasswordLists
            // Dispatch next available job in trackedCrunchList
            for (TrackedCrunchList trackedCrunchList : request.getTrackedCrunchLists()) {
                if (trackedCrunchList.getStatus() == TrackingStatus.PENDING) {
                    Job job = trackedCrunchListService.getNextJob(trackedCrunchList);
                    if (job != null)
                        return new RequestIO.GetJob.Response(
                                request.getRequestType(),
                                getRequestDetail(request),
                                trackedCrunchList.getId(),
                                job.getIndexNumber(),
                                job.getValues());
                }
            }

            // No jobs to dispatch from trackedCrunchListEither
            // If all trackedPasswordLists and all trackedCrunchLists are either ERROR or COMPLETE
            if (request.getTrackedPasswordLists().stream()
                    .allMatch(trackedPasswordList -> trackedPasswordList.getStatus() == TrackingStatus.COMPLETE
                            || trackedPasswordList.getStatus() == TrackingStatus.ERROR) &&
                    request.getTrackedCrunchLists().stream()
                            .allMatch(trackedCrunchList -> trackedCrunchList.getStatus() == TrackingStatus.COMPLETE
                                    || trackedCrunchList.getStatus() == TrackingStatus.ERROR)) {
                // TODO : Mark Request As Complete
                throw new SystemException(2423, "Request with id" + request.getId() + " is complete", NO_CONTENT);
            }
        }
        // No more job exception
        throw new SystemException(3242, "No Jobs Available", NO_CONTENT);
    }

    public void reportJob(Long id, String jobId, Boolean success, String password) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(id, user);

        if (success) {
            // Password Found!
            if (password == null || password.isEmpty())
                throw new SystemException(32423, "Success report must have a value to report", BAD_REQUEST);
            // Retire Active Request
            retireActiveRequest(request.getId());
            // TODO : Email Password Found
        } else {
            // Job Complete. Password was not found... Mark Job as Complete
            TrackedJob trackedJob = trackedPasswordListJobService.getTrackedJob(request, jobId);
            switch (trackedJob.getStatus()) {
                case PENDING:
                    throw new SystemException(342, "Tracked Job has pending status and cannot be completed!", BAD_REQUEST);
                case COMPLETE:
                    throw new SystemException(342, "Tracked Job has complete status and cannot be completed!", BAD_REQUEST);
                case ERROR:
                    throw new SystemException(342, "Tracked Job has error status and cannot be completed!", BAD_REQUEST);
            }
            trackedPasswordListJobService.markJobAsComplete(trackedJob);

            // Check if there are more jobs left
            if (trackedPasswordListJobService.getNextTrackedJobForRequest(request) == null) {
                // No more Jobs are Left... retire active request
                retireActiveRequest(request.getId());
                // TODO : Email Password Not Found
            }
        }
    }

    public void retireActiveRequest(Long id) {
        Request request = requestPermissionLayer.get(id);
        switch (request.getRequestType()) {
            case WPA:
                wpaRequestDetailService.delete(id);
        }
        requestRepository.delete(request);
        // TODO : Move to Completed Request
    }

    private RequestDetail getRequestDetail(Request request) {
        switch (request.getRequestType()) {
            case WPA:
                return wpaRequestDetailService.get(request.getId());
            case MATCH:
                return matchRequestDetailService.get(request.getId());
            default:
                throw new RuntimeException("Request Type was null");
        }
    }
}

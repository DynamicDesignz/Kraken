package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.TrackedCrunchList;
import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.embedded.Job;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
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
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static org.zalando.problem.Status.NO_CONTENT;

@Service
@Transactional
public class RequestService {

    private UserService userService;
    private RequestPermissionLayer requestPermissionLayer;
    private RequestRepository requestRepository;
    private WPARequestDetailService wpaRequestDetailService;
    private MatchRequestDetailService matchRequestDetailService;
    private TrackedPasswordListService trackedPasswordListService;
    private TrackedCrunchListService trackedCrunchListService;

    public RequestService(UserService userService,
                          RequestPermissionLayer requestPermissionLayer,
                          RequestRepository requestRepository,
                          WPARequestDetailService wpaRequestDetailService,
                          MatchRequestDetailService matchRequestDetailService,
                          TrackedPasswordListService trackedPasswordListService,
                          TrackedCrunchListService trackedCrunchListService) {
        this.userService = userService;
        this.requestPermissionLayer = requestPermissionLayer;
        this.requestRepository = requestRepository;
        this.wpaRequestDetailService = wpaRequestDetailService;
        this.matchRequestDetailService = matchRequestDetailService;
        this.trackedPasswordListService = trackedPasswordListService;
        this.trackedCrunchListService = trackedCrunchListService;
    }

    public Request create(RequestIO.Create.Request requestDTO, MultipartFile passwordCaptureFile) {
        User user = userService.getUserOrThrow();
        // Init Request
        Request request = requestRepository.save(new Request());
        request.setOwner(user);

        // Set Request Detail
        switch (requestDTO.getRequestType()) {
            case WPA:
                request.setRequestDetail(
                        wpaRequestDetailService.create((WPARequestDetail) requestDTO.getRequestDetail(), passwordCaptureFile));
                request.setRequestType(RequestType.WPA);
                break;
            case MATCH:
                request.setRequestDetail(
                        matchRequestDetailService.create((MatchRequestDetail) requestDTO.getRequestDetail()));
                request.setRequestType(RequestType.MATCH);
                break;
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
        request.setRequestDetail(getRequestDetail(request));
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
                                request.getId(),
                                trackedPasswordList.getId(),
                                RequestIO.Common.TrackedListType.PASSWORD_LIST,
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
                                request.getId(),
                                trackedCrunchList.getId(),
                                RequestIO.Common.TrackedListType.CRUNCH,
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

    public void reportJob(RequestIO.ReportJob.Request requestDTO) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(requestDTO.getRequestId(), user);

        // If success
        if (requestDTO.getResult() != null && !requestDTO.getResult().isEmpty()){
            // TODO Found Password, Kill Request
        }
        // Else
        else
            switch (requestDTO.getTrackedListType()){
                case CRUNCH:
                    TrackedCrunchList trackedCrunchList = request.getTrackedCrunchLists().stream()
                            .filter(list -> Objects.equals(list.getId(), requestDTO.getListId()))
                            .findFirst()
                            .orElseThrow(() -> new SystemException(32,
                                    "Crunch List with id " + requestDTO.getListId() + " not found", NOT_FOUND));
                    trackedCrunchListService.reportJob(trackedCrunchList, requestDTO.getJobIndexNumber(), requestDTO.getTrackingStatus());
                    break;
                case PASSWORD_LIST:
                    TrackedPasswordList trackedPasswordList = request.getTrackedPasswordLists().stream()
                            .filter(list -> Objects.equals(list.getId(), requestDTO.getListId()))
                            .findFirst()
                            .orElseThrow(() -> new SystemException(32,
                                    "Password List with id " + requestDTO.getListId() + " not found", NOT_FOUND));
                    trackedPasswordListService.reportJob(trackedPasswordList, requestDTO.getJobIndexNumber(), requestDTO.getTrackingStatus());
                    break;
            }

        // Check if request is complete
        if (request.getTrackedPasswordLists().stream()
                .allMatch(trackedPasswordList -> trackedPasswordList.getStatus() == TrackingStatus.COMPLETE
                        || trackedPasswordList.getStatus() == TrackingStatus.ERROR) &&
                request.getTrackedCrunchLists().stream()
                        .allMatch(trackedCrunchList -> trackedCrunchList.getStatus() == TrackingStatus.COMPLETE
                                || trackedCrunchList.getStatus() == TrackingStatus.ERROR)) {
            // TODO : Mark Request As Complete
        }
    }

    public void retireActiveRequest(Long id) {
        Request request = requestPermissionLayer.get(id);
        switch (request.getRequestType()) {
            case WPA:
                wpaRequestDetailService.delete(id);
                break;
            case MATCH:
                matchRequestDetailService.delete(id);
                break;
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

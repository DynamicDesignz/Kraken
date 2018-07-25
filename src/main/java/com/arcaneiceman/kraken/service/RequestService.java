package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.domain.Job;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import com.arcaneiceman.kraken.domain.request.detail.WPARequestDetail;
import com.arcaneiceman.kraken.repository.RequestRepository;
import com.arcaneiceman.kraken.service.permission.abs.RequestPermissionLayer;
import com.arcaneiceman.kraken.service.request.detail.MatchRequestDetailService;
import com.arcaneiceman.kraken.service.request.detail.WPARequestDetailService;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.arcaneiceman.kraken.config.Constants.WORKER_NAME;
import static com.arcaneiceman.kraken.config.Constants.WORKER_TYPE;
import static org.zalando.problem.Status.NO_CONTENT;

@Service
@Transactional
public class RequestService {

    private UserService userService;
    private WorkerService workerService;
    private RequestPermissionLayer requestPermissionLayer;
    private RequestRepository requestRepository;
    private WPARequestDetailService wpaRequestDetailService;
    private MatchRequestDetailService matchRequestDetailService;
    private TrackedListService trackedListService;

    public RequestService(UserService userService,
                          WorkerService workerService, RequestPermissionLayer requestPermissionLayer,
                          RequestRepository requestRepository,
                          WPARequestDetailService wpaRequestDetailService,
                          MatchRequestDetailService matchRequestDetailService,
                          TrackedListService trackedListService) {
        this.userService = userService;
        this.workerService = workerService;
        this.requestPermissionLayer = requestPermissionLayer;
        this.requestRepository = requestRepository;
        this.wpaRequestDetailService = wpaRequestDetailService;
        this.matchRequestDetailService = matchRequestDetailService;
        this.trackedListService = trackedListService;
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

        // Create Tracked Password Lists
        requestDTO.getPasswordLists().forEach(passwordListName ->
                trackedListService.createPasswordList(passwordListName, request));

        // Create Tracked Crunch Lists
        requestDTO.getCrunchParams().forEach(crunchParams ->
                trackedListService.createCrunchList(
                        crunchParams.getMinSize(),
                        crunchParams.getMaxSize(),
                        crunchParams.getCharacters(),
                        crunchParams.getStartString(),
                        request));

        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Request get(Long id) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(id, user);
        request.setRequestDetail(getRequestDetail(request));
        request.getTrackedLists().size();
        return request;
    }

    public RequestIO.GetJob.Response getJob(HttpServletRequest httpServletRequest) {
        User user = userService.getUserOrThrow();
        String workerName = (String) httpServletRequest.getAttribute(WORKER_NAME);
        WorkerType workerType = (WorkerType) httpServletRequest.getAttribute(WORKER_TYPE);
        Worker worker = workerService.get(workerName, workerType);
        // Get All Requests
        List<Request> requestList = requestRepository.findByOwner(user);

        for (Request request : requestList) {
            for (TrackedList trackedList : request.getTrackedLists()) {
                if (trackedList.getStatus() == TrackingStatus.PENDING) {
                    Job job = trackedListService.getNextJob(trackedList, worker);
                    if (job != null)
                        return new RequestIO.GetJob.Response(
                                request.getRequestType(),
                                getRequestDetail(request),
                                request.getId(),
                                trackedList.getId(),
                                job.getId(),
                                job.getValues());
                }
            }

            // No jobs to dispatch from trackedLists.... check if all are either ERROR or COMPLETE
            if (request.getTrackedLists().stream().allMatch(trackedList ->
                    trackedList.getStatus() == TrackingStatus.COMPLETE
                            || trackedList.getStatus() == TrackingStatus.ERROR)) {
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

        if (requestDTO.getResult() != null && !requestDTO.getResult().isEmpty()) {
            // TODO : Mark Request As Complete
        }
        else
            trackedListService.reportJob(requestDTO.getListId(), requestDTO.getJobId(),
                    requestDTO.getTrackingStatus(), request);

        // Check if request is complete
        if (request.getTrackedLists().stream().allMatch(trackedList ->
                trackedList.getStatus() == TrackingStatus.COMPLETE
                        || trackedList.getStatus() == TrackingStatus.ERROR)) {
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

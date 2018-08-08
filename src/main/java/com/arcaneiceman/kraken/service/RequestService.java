package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.domain.Job;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.domain.enumerations.WorkerStatus;
import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import com.arcaneiceman.kraken.domain.request.detail.WPARequestDetail;
import com.arcaneiceman.kraken.repository.RequestRepository;
import com.arcaneiceman.kraken.service.permission.abs.RequestPermissionLayer;
import com.arcaneiceman.kraken.service.request.detail.RequestDetailService;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.zalando.problem.Status.BAD_REQUEST;

@Service
@Transactional
public class RequestService {

    private UserService userService;
    private WorkerService workerService;
    private RequestPermissionLayer requestPermissionLayer;
    private RequestRepository requestRepository;
    private RequestDetailService requestDetailService;
    private TrackedListService trackedListService;
    private ResultService resultService;

    public RequestService(UserService userService,
                          WorkerService workerService, RequestPermissionLayer requestPermissionLayer,
                          RequestRepository requestRepository,
                          RequestDetailService requestDetailService, TrackedListService trackedListService, ResultService resultService) {
        this.userService = userService;
        this.workerService = workerService;
        this.requestPermissionLayer = requestPermissionLayer;
        this.requestRepository = requestRepository;
        this.requestDetailService = requestDetailService;
        this.trackedListService = trackedListService;
        this.resultService = resultService;
    }

    public Request create(RequestIO.Create.Request requestDTO, MultipartFile passwordCaptureFile) {
        User user = userService.getUserOrThrow();
        // Init Request
        Request request = requestRepository.save(new Request());
        request.setOwner(user);

        // Set Request Detail
        switch (requestDTO.getRequestType()) {
            case WPA:
                request.setRequestDetail(requestDetailService.createWPARequest(
                        (WPARequestDetail) requestDTO.getRequestDetail(),
                        passwordCaptureFile));
                request.setRequestType(RequestType.WPA);
                break;
            case MATCH:
                request.setRequestDetail(requestDetailService.createMatchRequest(
                        (MatchRequestDetail) requestDTO.getRequestDetail()));
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
        request.setRequestDetail(requestDetailService.get(request.getRequestType(), request.getRequestDetail().getId()));
        request.setTotalJobCount(request.getTrackedLists().stream().mapToInt(TrackedList::getTotalJobCount).sum());
        request.setErrorJobCount(request.getTrackedLists().stream().mapToInt(TrackedList::getErrorJobCount).sum());
        request.setCompletedJobCount(request.getTrackedLists().stream().mapToInt(TrackedList::getCompletedJobCount).sum());
        return request;
    }

    public Page<Request> get(Pageable pageable) {
        User user = userService.getUserOrThrow();
        return requestRepository.findByOwner(pageable, user);
    }

    public RequestIO.GetJob.Response getJob(HttpServletRequest httpServletRequest) {
        User user = userService.getUserOrThrow();
        Worker worker = workerService.get(httpServletRequest);
        if (worker.getJob() != null)
            throw new SystemException(324, "Worker already has job", BAD_REQUEST);
        if (worker.getStatus() == WorkerStatus.OFFLINE)
            throw new SystemException(242, "Worker is currently OFFLINE", BAD_REQUEST);

        // Get All Requests
        List<Request> requestList = requestRepository.findByOwner(user);

        for (Request request : requestList) {
            for (TrackedList trackedList : request.getTrackedLists()) {
                if (trackedList.getStatus() == TrackingStatus.PENDING || trackedList.getStatus() == TrackingStatus.RUNNING) {
                    Job job = trackedListService.getNextJob(trackedList, worker);
                    if (job != null)
                        return new RequestIO.GetJob.Response(
                                request.getRequestType(),
                                requestDetailService.get(request.getRequestType(), request.getRequestDetail().getId()),
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
                retireRequest(request.getId());
                throw new SystemException(2423, "Request with id" + request.getId() + " is complete", BAD_REQUEST);
            }
        }
        // No more job exception
        throw new SystemException(3242, "No Jobs Available", BAD_REQUEST);
    }

    public void reportJob(RequestIO.ReportJob.Request requestDTO, HttpServletRequest httpServletRequest) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(requestDTO.getRequestId(), user);
        Worker worker = workerService.get(httpServletRequest);
        if (worker.getJob() == null)
            throw new SystemException(324, "Worker does not have a job to report", BAD_REQUEST);
        if (worker.getStatus() == WorkerStatus.OFFLINE)
            throw new SystemException(242, "Worker is currently OFFLINE", BAD_REQUEST);

        if (requestDTO.getResult() != null && !requestDTO.getResult().isEmpty()) {
            retireRequest(request.getId(), requestDTO.getResult());
        } else
            trackedListService.reportJob(requestDTO.getJobId(), requestDTO.getListId(), request,
                    requestDTO.getTrackingStatus(), worker);

        // Check if request is complete
        if (request.getTrackedLists().stream().allMatch(trackedList ->
                trackedList.getStatus() == TrackingStatus.COMPLETE
                        || trackedList.getStatus() == TrackingStatus.ERROR)) {
            retireRequest(request.getId());
        }
    }

    public void retireRequest(Long id, String value) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(id, user);
        resultService.create(request.getRequestType(),
                request.getRequestDetail(),
                value,
                request.getTrackedLists().stream().mapToInt(TrackedList::getTotalJobCount).sum(),
                request.getTrackedLists().stream().mapToInt(TrackedList::getErrorJobCount).sum(),
                request.getTrackedLists().stream().mapToInt(TrackedList::getCompletedJobCount).sum());
        requestRepository.delete(request);
        // TODO : Send Email
    }

    public void retireRequest(Long id) {
        retireRequest(id, null);
    }
}

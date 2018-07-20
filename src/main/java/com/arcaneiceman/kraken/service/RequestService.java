package com.arcaneiceman.kraken.service;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.domain.*;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.repository.RequestRepository;
import com.arcaneiceman.kraken.service.permission.abs.RequestPermissionLayer;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import com.ttt.eru.libs.fileupload.configuration.AmazonS3Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

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
    private TrackedPasswordListJobService trackedPasswordListJobService;
    private WPARequestDetailService wpaRequestDetailService;

    public RequestService(UserService userService,
                          AmazonS3Configuration amazonS3Configuration,
                          PasswordListService passwordListService,
                          RequestPermissionLayer requestPermissionLayer,
                          RequestRepository requestRepository,
                          TrackedPasswordListJobService trackedPasswordListJobService,
                          WPARequestDetailService wpaRequestDetailService) {
        this.userService = userService;
        this.amazonS3Configuration = amazonS3Configuration;
        this.passwordListService = passwordListService;
        this.requestPermissionLayer = requestPermissionLayer;
        this.requestRepository = requestRepository;
        this.trackedPasswordListJobService = trackedPasswordListJobService;
        this.wpaRequestDetailService = wpaRequestDetailService;
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
                                 MultipartFile passwordCaptureFile,
                                 String[] candidateValueLists) {
        User user = userService.getUserOrThrow();
        // Init Request
        Request request = requestRepository.save(new Request());
        request.setOwner(user);

        // Set Request Detail
        switch (requestDTO.getRequestType()) {
            case WPA:
                request.setRequestDetail(
                        wpaRequestDetailService.create((WPARequestDetail) requestDTO.getRequestDetail(), passwordCaptureFile));
        }

        // Set Candidate Value List Set
        request.setCandidateValueListSet(new HashSet<>(Arrays.asList(candidateValueLists)));

        // Set Tracked Jobs
        request.setTrackedJobSet(new HashSet<>());
        // For Candidate Value List...
        request.getCandidateValueListSet().forEach(candidateValueListName -> {
            // Get Candidate Value List from Service
            PasswordList passwordList = passwordListService.get(candidateValueListName);
            if (passwordList == null)
                throw new SystemException(23423,
                        "Candidate Value List with name " + candidateValueListName + " not found", BAD_REQUEST);
            // For Each Job Delimiter, Add a Tracked Job to the Request
            passwordList.getJobDelimiterSet().forEach(jobDelimter -> {
                TrackedJob trackedJob = TrackedJob.builder()
                        .id(UUID.randomUUID().toString())
                        .startByte(jobDelimter.getStartByte())
                        .endByte(jobDelimter.getEndByte())
                        .status(TrackingStatus.PENDING)
                        .candidateValueListName(passwordList.getName())
                        .candidateValueListCharset(passwordList.getCharset())
                        .build();
                trackedJob.setOwner(request);
                request.getTrackedJobSet().add(trackedJob);
            });
        });

        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Request get(Long id) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(id, user);
        request.getCandidateValueListSet().size();
        return request;
    }

    public RequestIO.GetJob.Response getJob(Long id) {
        User user = userService.getUserOrThrow();
        Request request = requestPermissionLayer.getWithOwner(id, user);
        TrackedJob trackedJob = trackedPasswordListJobService.getNextTrackedJobForRequest(request);
        if (trackedJob == null)
            throw new SystemException(1231, "No further pending jobs left", BAD_REQUEST);

        // Declare Variables
        List<String> candidateValues = new ArrayList<>();

        // Get Portion of the Candidate Value List
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(amazonS3Configuration.getAmazonS3BucketName(),
                    candidateValueListStoragePath + "/" + trackedJob.getCandidateValueListName());
            getObjectRequest.setRange(trackedJob.getStartByte(), trackedJob.getEndByte());
            S3Object object = amazonS3Configuration.generateClient().getObject(getObjectRequest);
            InputStream fileStream = new BufferedInputStream(object.getObjectContent());
            InputStreamReader decoder = new InputStreamReader(fileStream, trackedJob.getCandidateValueListCharset());
            BufferedReader buffered = new BufferedReader(decoder);

            String thisLine;
            while ((thisLine = buffered.readLine()) != null)
                candidateValues.add(thisLine);
        } catch (Exception e) {
            // Failed to retrieve Candidate Value List... marking job as error
            trackedPasswordListJobService.markJobAsError(trackedJob);
            throw new SystemException(2432, "Failed to retrieve candidate values. Marking Job as error", INTERNAL_SERVER_ERROR);
        }

        // Mark Job As Running
        trackedJob = trackedPasswordListJobService.markJobAsRunning(trackedJob);

        // Send Response
        return new RequestIO.GetJob.Response(request.getRequestType(), request.getRequestDetail(), trackedJob.getId(), candidateValues);
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
        switch (request.getRequestType()){
            case WPA:
                wpaRequestDetailService.delete(id);
        }
        requestRepository.delete(request);
        // TODO : Move to Completed Request
    }
}

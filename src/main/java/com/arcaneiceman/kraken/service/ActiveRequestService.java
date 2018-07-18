package com.arcaneiceman.kraken.service;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.arcaneiceman.kraken.controller.io.ActiveRequestIO;
import com.arcaneiceman.kraken.domain.ActiveRequest;
import com.arcaneiceman.kraken.domain.CandidateValueList;
import com.arcaneiceman.kraken.domain.TrackedJob;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.enumerations.TrackedJobStatus;
import com.arcaneiceman.kraken.repository.ActiveRequestRepository;
import com.arcaneiceman.kraken.service.permission.abs.ActiveRequestPermissionLayer;
import com.arcaneiceman.kraken.service.utils.FileUploadService;
import com.arcaneiceman.kraken.util.ConsoleCommandUtil;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import com.ttt.eru.libs.fileupload.configuration.AmazonS3Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.arcaneiceman.kraken.config.Constants.*;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Service
@Transactional
public class ActiveRequestService {

    @Value("${application.kraken-request-settings.validation-prefix}")
    private String passwordCaptureValidationPath;

    @Value("${application.kraken-request-settings.folder-prefix}")
    private String passwordCaptureStoragePath;

    @Value("${application.candidate-value-list-settings.folder-prefix}")
    private String candidateValueListStoragePath;

    private UserService userService;
    private AmazonS3Configuration amazonS3Configuration;
    private CandidateValueListService candidateValueListService;
    private ActiveRequestPermissionLayer activeRequestPermissionLayer;
    private ActiveRequestRepository activeRequestRepository;
    private FileUploadService fileUploadService;
    private TrackedJobService trackedJobService;

    public ActiveRequestService(UserService userService, AmazonS3Configuration amazonS3Configuration,
                                CandidateValueListService candidateValueListService,
                                ActiveRequestPermissionLayer activeRequestPermissionLayer,
                                ActiveRequestRepository activeRequestRepository,
                                FileUploadService fileUploadService,
                                TrackedJobService trackedJobService) {
        this.userService = userService;
        this.amazonS3Configuration = amazonS3Configuration;
        this.candidateValueListService = candidateValueListService;
        this.activeRequestPermissionLayer = activeRequestPermissionLayer;
        this.activeRequestRepository = activeRequestRepository;
        this.fileUploadService = fileUploadService;
        this.trackedJobService = trackedJobService;
    }

    @PostConstruct
    public void checkValues() {
        if (candidateValueListStoragePath == null || candidateValueListStoragePath.isEmpty())
            throw new RuntimeException("Application Candidate Value List Fetcher : Storage Folder Not Specified ");
        if (passwordCaptureStoragePath == null || passwordCaptureStoragePath.isEmpty())
            throw new RuntimeException("Application Password Capture Fetcher : Storage Folder Not Specified");
    }

    public ActiveRequest createWPAActiveRequest(MultipartFile passwordCaptureFile,
                                                String ssid,
                                                String[] candidateValueLists) {
        User user = userService.getUserOrThrow();
        ActiveRequest activeRequest = activeRequestRepository.save(new ActiveRequest());

        // Validate Capture file
        Path tempFilePath = Paths.get(passwordCaptureValidationPath, UUID.randomUUID().toString());
        try {
            if (!Files.exists(tempFilePath))
                Files.createDirectories(tempFilePath.getParent());
            String response = ConsoleCommandUtil.executeCommandInConsole(
                    "aircrack-ng", tempFilePath.toString(), "-b", ssid);
            if (!response.contains(VALID_FILE))
                if (response.contains(INVALID_SSID))
                    throw new SystemException(2342, "SSID was not found in the capture", BAD_REQUEST);
                else if (response.contains(INVALID_FILE))
                    throw new SystemException(234, "Could not understand capture file", BAD_REQUEST);

        } catch (IOException | InterruptedException ignored) {
            throw new SystemException(21312, "Error Processing the capture file", INTERNAL_SERVER_ERROR);
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ignored) {
            }
        }

        // Upload passwordCaptureFile
        String fileKey = fileUploadService.uploadFile(passwordCaptureFile, passwordCaptureStoragePath + "/");

        // Init Kraken Request
        activeRequest.setOwner(user);
        activeRequest.setSsid(ssid);
        activeRequest.setPasswordCaptureFileKey(fileKey.substring(fileKey.lastIndexOf("/") + 1));
        activeRequest.setCandidateValueListSet(new HashSet<>(Arrays.asList(candidateValueLists)));
        activeRequest.setTrackedJobSet(new HashSet<>());

        // For Candidate Value List...
        activeRequest.getCandidateValueListSet().forEach(candidateValueListName -> {
            // Get Candidate Value List from Service
            CandidateValueList candidateValueList = candidateValueListService.get(candidateValueListName);
            if (candidateValueList == null)
                throw new SystemException(23423,
                        "Candidate Value List with name " + candidateValueListName + " not found", BAD_REQUEST);

            // For Each Job Delimiter, Add a Tracked Job to the Request
            candidateValueList.getJobDelimiterSet().forEach(jobDelimter -> {
                TrackedJob trackedJob = TrackedJob.builder()
                        .id(UUID.randomUUID().toString())
                        .startByte(jobDelimter.getStartByte())
                        .endByte(jobDelimter.getEndByte())
                        .status(TrackedJobStatus.PENDING)
                        .candidateValueListName(candidateValueList.getName())
                        .candidateValueListCharset(candidateValueList.getCharset())
                        .build();
                trackedJob.setOwner(activeRequest);
                activeRequest.getTrackedJobSet().add(trackedJob);
            });
        });

        return activeRequestRepository.save(activeRequest);
    }

    @Transactional(readOnly = true)
    public ActiveRequest getActiveRequest(Long id) {
        User user = userService.getUserOrThrow();
        ActiveRequest activeRequest = activeRequestPermissionLayer.getWithOwner(id, user);
        activeRequest.getCandidateValueListSet().size();
        return activeRequest;
    }

    public ActiveRequestIO.GetJob.Response getJob(Long id) {
        User user = userService.getUserOrThrow();
        ActiveRequest activeRequest = activeRequestPermissionLayer.getWithOwner(id, user);
        TrackedJob trackedJob = trackedJobService.getNextTrackedJobForRequest(activeRequest);
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
            trackedJobService.markJobAsError(trackedJob);
            throw new SystemException(2432, "Failed to retrieve candidate values. Marking Job as error", INTERNAL_SERVER_ERROR);
        }

        // Get Active Link for Password Capture File
        String link = fileUploadService.getLink(passwordCaptureStoragePath + "/" + activeRequest.getPasswordCaptureFileKey());

        // Mark Job As Running
        trackedJobService.markJobAsRunning(trackedJob);

        return new ActiveRequestIO.GetJob.Response(trackedJob.getId(), candidateValues, link);
    }

    public void reportJob(Long id, String jobId, Boolean success, String password) {
        User user = userService.getUserOrThrow();
        ActiveRequest activeRequest = activeRequestPermissionLayer.getWithOwner(id, user);

        if (success) {
            // Password Found!
            if (password == null || password.isEmpty())
                throw new SystemException(32423, "Success report must have a value to report", BAD_REQUEST);
            // Retire Active Request
            retireActiveRequest(activeRequest.getId());
            // TODO : Email Password Found
        } else {
            // Job Complete. Password was not found... Mark Job as Complete
            TrackedJob trackedJob = trackedJobService.getTrackedJob(activeRequest, jobId);
            switch (trackedJob.getStatus()) {
                case PENDING:
                    throw new SystemException(342, "Tracked Job has pending status and cannot be completed!", BAD_REQUEST);
                case COMPLETE:
                    throw new SystemException(342, "Tracked Job has complete status and cannot be completed!", BAD_REQUEST);
                case ERROR:
                    throw new SystemException(342, "Tracked Job has error status and cannot be completed!", BAD_REQUEST);
            }
            trackedJobService.markJobAsComplete(trackedJob);

            // Check if there are more jobs left
            if (trackedJobService.getNextTrackedJobForRequest(activeRequest) == null) {
                // No more Jobs are Left... retire active request
                retireActiveRequest(activeRequest.getId());
                // TODO : Email Password Not Found
            }
        }
    }

    public void retireActiveRequest(Long id) {
        ActiveRequest activeRequest = activeRequestPermissionLayer.get(id);
        if (activeRequest.getPasswordCaptureFileKey() != null)
            fileUploadService.deleteFile(activeRequest.getPasswordCaptureFileKey());
        activeRequestRepository.delete(activeRequest);

        // TODO : Move to Completed Request
    }
}

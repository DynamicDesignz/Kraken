package com.arcaneiceman.kraken.service.request.detail;

import com.arcaneiceman.kraken.domain.request.detail.WPARequestDetail;
import com.arcaneiceman.kraken.repository.WPARequestDetailRepository;
import com.arcaneiceman.kraken.service.utils.FileUploadService;
import com.arcaneiceman.kraken.util.ConsoleCommandUtil;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.arcaneiceman.kraken.config.Constants.*;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

@Service
@Transactional
public class WPARequestDetailService {

    @Value("${application.kraken-request-settings.validation-prefix}")
    private String passwordCaptureValidationPath;

    @Value("${application.kraken-request-settings.folder-prefix}")
    private String passwordCaptureStoragePath;

    private WPARequestDetailRepository wpaRequestDetailRepository;
    private FileUploadService fileUploadService;

    public WPARequestDetailService(WPARequestDetailRepository wpaRequestDetailRepository,
                                   FileUploadService fileUploadService) {
        this.wpaRequestDetailRepository = wpaRequestDetailRepository;
        this.fileUploadService = fileUploadService;
    }

    @PostConstruct
    public void checkValues() {
        if (passwordCaptureValidationPath == null || passwordCaptureValidationPath.isEmpty())
            throw new RuntimeException("Application WPA Request Detail Service - Password Capture Validation Path Error :" +
                    "Storage Path Not Specified ");
        if (passwordCaptureStoragePath == null || passwordCaptureStoragePath.isEmpty())
            throw new RuntimeException("Application WPA Request Detail Service - Password Capture Storage Path Error :" +
                    "Storage Path Not Specified");
    }

    public WPARequestDetail create(WPARequestDetail wpaRequestDetail, MultipartFile passwordCaptureFile) {
        // Clear any id if present
        wpaRequestDetail.setId(null);

        String validationErrorMessage = null;

        // Validate Capture File using Aircrack
        Path tempFilePath = Paths.get(passwordCaptureValidationPath, UUID.randomUUID().toString());
        try {
            if (!Files.exists(tempFilePath))
                Files.createDirectories(tempFilePath.getParent());
            Files.write(tempFilePath, passwordCaptureFile.getBytes());
            String response = ConsoleCommandUtil.executeCommandInConsole(
                    1, TimeUnit.SECONDS,
                    ConsoleCommandUtil.OutputStream.OUT,
                    "aircrack-ng", tempFilePath.toString(), "-b", wpaRequestDetail.getSsid());
            if (response == null)
                validationErrorMessage = "Did not get response from aircrack";
            else if (!response.contains(VALID_FILE))
                if (response.contains(INVALID_BSSID))
                    validationErrorMessage = "Network name was invalid";
                else if (response.contains(INVALID_FILE))
                    validationErrorMessage = "Aircrack did not recognize this file format";
                else
                    validationErrorMessage = "Unknown Error while processing pcap file";
        } catch (Exception ignored) {
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ignored) {
            }
        }

        if (validationErrorMessage != null)
            throw new SystemException(22342, validationErrorMessage, BAD_REQUEST);

        String fileKey = fileUploadService.uploadFile(passwordCaptureFile, passwordCaptureStoragePath + "/");
        wpaRequestDetail.setPasswordCaptureFileKey(fileKey);

        // Save and return
        return wpaRequestDetailRepository.save(wpaRequestDetail);
    }

    @Transactional(readOnly = true)
    public WPARequestDetail get(Long id) {
        WPARequestDetail wpaRequestDetail = wpaRequestDetailRepository.findById(id)
                .orElseThrow(() -> new SystemException(34, "WPA Request with id " + id + " not found", NOT_FOUND));
        wpaRequestDetail.setPasswordCaptureFileUrl(fileUploadService.getLink(wpaRequestDetail.getPasswordCaptureFileKey()));
        return wpaRequestDetail;
    }

    public void delete(Long id) {
        WPARequestDetail wpaRequestDetail = wpaRequestDetailRepository.findById(id)
                .orElseThrow(() -> new SystemException(34, "WPA Request with id " + id + " not found", NOT_FOUND));
        if (wpaRequestDetail.getPasswordCaptureFileKey() != null)
            fileUploadService.deleteFile(wpaRequestDetail.getPasswordCaptureFileKey());
        wpaRequestDetailRepository.delete(wpaRequestDetail);
    }


}

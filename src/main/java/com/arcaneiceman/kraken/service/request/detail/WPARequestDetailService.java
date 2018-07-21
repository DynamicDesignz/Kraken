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

import static com.arcaneiceman.kraken.config.Constants.*;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

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

        // Validate Capture file and Upload File it
        Path tempFilePath = Paths.get(passwordCaptureValidationPath, UUID.randomUUID().toString());
        try {
            if (!Files.exists(tempFilePath))
                Files.createDirectories(tempFilePath.getParent());
            Files.write(tempFilePath, passwordCaptureFile.getBytes());
            String response = ConsoleCommandUtil.executeCommandInConsole(
                    "aircrack-ng", tempFilePath.toString(), "-b", wpaRequestDetail.getSsid());
            if (!response.contains(VALID_FILE))
                if (response.contains(INVALID_SSID))
                    throw new SystemException(2342, "SSID was not found in the capture", BAD_REQUEST);
                else if (response.contains(INVALID_FILE))
                    throw new SystemException(234, "Could not understand capture file", BAD_REQUEST);

        } catch (IOException ignored) {
            throw new SystemException(21312, "Error Processing the capture file", INTERNAL_SERVER_ERROR);
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ignored) {
            }
        }
        String fileKey = fileUploadService.uploadFile(passwordCaptureFile, passwordCaptureStoragePath + "/");
        wpaRequestDetail.setPasswordCaptureFileKey(fileKey);

        // Save and return
        return wpaRequestDetailRepository.save(wpaRequestDetail);
    }

    @Transactional(readOnly = true)
    public WPARequestDetail get(Long id) {
        WPARequestDetail wpaRequestDetail = wpaRequestDetailRepository.getOne(id);
        wpaRequestDetail.setPasswordCaptureFileUrl(fileUploadService.getLink(wpaRequestDetail.getPasswordCaptureFileKey()));
        return wpaRequestDetail;
    }

    public void delete(Long id) {
        WPARequestDetail wpaRequestDetail = wpaRequestDetailRepository.getOne(id);
        if (wpaRequestDetail.getPasswordCaptureFileKey() != null)
            fileUploadService.deleteFile(wpaRequestDetail.getPasswordCaptureFileKey());
        wpaRequestDetailRepository.delete(wpaRequestDetail);
    }


}

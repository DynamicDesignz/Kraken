package com.wali.kraken.controllers;

import com.wali.kraken.core.ProcessingCore;
import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.passwordrequests.CompletedPasswordRequestsRepository;
import com.wali.kraken.repositories.passwordrequests.PendingPasswordRequestsRepository;
import com.wali.kraken.services.ServiceFunctions;
import com.wali.kraken.utils.Exceptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController("/password-request")
public class PasswordRequestController {

    private ExecutorService executorService;
    private ProcessingCore processingCore;
    private ServiceFunctions serviceFunctions;
    private PendingPasswordRequestsRepository pendingPasswordRequestsRepository;
    private CompletedPasswordRequestsRepository completedPasswordRequestsRepository;

    @Autowired
    public PasswordRequestController(ServiceFunctions serviceFunctions,
                                     ProcessingCore processingCore,
                                     PendingPasswordRequestsRepository pendingPasswordRequestsRepository,
                                     CompletedPasswordRequestsRepository completedPasswordRequestsRepository){
        this.serviceFunctions = serviceFunctions;
        this.pendingPasswordRequestsRepository = pendingPasswordRequestsRepository;
        this.completedPasswordRequestsRepository = completedPasswordRequestsRepository;
        this.processingCore = processingCore;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // Create Password Request
    @PostMapping
    public ResponseEntity<PasswordRequest> createPasswordRequest(
            @RequestPart(value = "passwordcapturefile") MultipartFile passwordCaptureFile,
            @RequestParam(value = "ssid") String ssid,
            @RequestParam(value = "passwordlist") String[] passwordLists){

        // Check if password file is valid
        byte[] passwordCaptureFileBytes;
        try{
            passwordCaptureFileBytes = passwordCaptureFile.getBytes();
            serviceFunctions.testForValidCrack(passwordCaptureFileBytes, ssid);
        } catch (IOException e) {
            throw new Exceptions.InvalidPasswordFileException(e.getCause());
        }

        // Create Password Request Object
        PasswordRequest passwordRequest = new PasswordRequest(
                null,
                ssid,
                Base64.getEncoder().encodeToString(passwordCaptureFileBytes),
                String.join(":", passwordLists),
                null);

        // Add them to repository
        pendingPasswordRequestsRepository.save(passwordRequest);

        // Submit and call processing core on another thread
        executorService.submit(() -> processingCore.process(null,
                null,
                null));

        // Return a response
        return new ResponseEntity<>(passwordRequest, new HttpHeaders(), HttpStatus.CREATED);
    }

    // Delete a Password Request
    @DeleteMapping
    public ResponseEntity<PasswordRequest> deletePasswordRequest(@RequestParam Long id){
//        PasswordRequest p = pendingPasswordRequestsRepository.findOne(id);
//        if (p != null)
//            return new ResponseEntity<>(p, new HttpHeaders(), HttpStatus.OK);
//        // else if ( jobmanger.getcurrentRequest().getId() == id)
//        // clear request;
//        // TODO : fill the above
//        else
//            throw new Exceptions.PasswordRequestNotFound();
        return null;
    }

    @GetMapping(value = "/get-pending-password-requests", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<PasswordRequest> getPendingPasswordRequests(){
        return pendingPasswordRequestsRepository.findAll();
    }

    @GetMapping(value = "/get-completed-password-requests", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<PasswordRequest> getCompletedPasswordRequests(){
        return completedPasswordRequestsRepository.findAll();
    }

    @GetMapping(value = "/test")
    public void test(){
        pendingPasswordRequestsRepository.save(
                new PasswordRequest(null,
                        "wali",
                        "walipash",
                        "default_list.txt",
                        null));
        executorService.submit(() -> processingCore.process(null,
                null,
                null));

    }

}

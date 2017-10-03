package com.wali.kraken.controllers;

import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.CompletedPasswordRequestsRepository;
import com.wali.kraken.repositories.PendingPasswordRequestsRepository;
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
import java.util.UUID;

@RestController("/password-request")
public class PasswordRequestController {

    private ServiceFunctions serviceFunctions;
    private PendingPasswordRequestsRepository pendingPasswordRequestsRepository;
    private CompletedPasswordRequestsRepository completedPasswordRequestsRepository;

    @Autowired
    public PasswordRequestController(ServiceFunctions serviceFunctions,
                                     PendingPasswordRequestsRepository pendingPasswordRequestsRepository,
                                     CompletedPasswordRequestsRepository completedPasswordRequestsRepository){
        this.serviceFunctions = serviceFunctions;
        this.pendingPasswordRequestsRepository = pendingPasswordRequestsRepository;
        this.completedPasswordRequestsRepository = completedPasswordRequestsRepository;
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
                UUID.randomUUID(),
                ssid,
                Base64.getEncoder().encodeToString(passwordCaptureFileBytes),
                String.join(":", passwordLists),
                null);

        // Add them to repository
        pendingPasswordRequestsRepository.save(passwordRequest);

        // TODO : Call Job Manager Loop

        // Return a response
        return new ResponseEntity<>(passwordRequest, new HttpHeaders(), HttpStatus.CREATED);
    }

    // Delete a Password Request
    @DeleteMapping
    public ResponseEntity<PasswordRequest> deletePasswordRequest(@RequestParam String id){
        PasswordRequest p = pendingPasswordRequestsRepository.findOne(id);
        if (p != null)
            return new ResponseEntity<>(p, new HttpHeaders(), HttpStatus.OK);
        // else if ( jobmanger.getcurrentRequest().getId() == id)
        // clear request;
        // TODO : fill the above
        else
            throw new Exceptions.PasswordRequestNotFound();
    }

    @RequestMapping(value = "/get-pending-password-requests", method = RequestMethod.GET,
    produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<PasswordRequest> getPendingPasswordRequests(){
        return pendingPasswordRequestsRepository.findAll();
    }

    @RequestMapping(value = "/get-completed-password-requests", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<PasswordRequest> getCompletedPasswordRequests(){
        return completedPasswordRequestsRepository.findAll();
    }

}

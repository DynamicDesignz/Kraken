package com.wali.kraken.controllers;

import com.wali.kraken.core.ProcessingCore;
import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.domain.core.CrackRequest;
import com.wali.kraken.enumerations.ProcessingStatus;
import com.wali.kraken.enumerations.RequestType;
import com.wali.kraken.repositories.CandidateValueListRepository;
import com.wali.kraken.repositories.CrackRequestRepository;
import com.wali.kraken.services.ServiceFunctions;
import com.wali.kraken.utils.KrakenException;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController("/crack-request")
public class CrackRequestController {

    private ExecutorService executorService;
    private ProcessingCore processingCore;
    private ServiceFunctions serviceFunctions;
    private CrackRequestRepository crackRequestRepository;
    private CandidateValueListRepository candidateValueListRepository;

    @Autowired
    public CrackRequestController(ServiceFunctions serviceFunctions,
                                  ProcessingCore processingCore,
                                  CrackRequestRepository crackRequestRepository,
                                  CandidateValueListRepository candidateValueListRepository) {
        this.serviceFunctions = serviceFunctions;
        this.processingCore = processingCore;
        this.executorService = Executors.newSingleThreadExecutor();
        this.crackRequestRepository = crackRequestRepository;
        this.candidateValueListRepository = candidateValueListRepository;
    }

    // Create WPA Password Request
    @PostMapping("/createWPARequest")
    public ResponseEntity<CrackRequest> createPasswordRequest(
            @RequestPart(value = "password-capture-file") MultipartFile passwordCaptureFile,
            @RequestParam(value = "ssid") String ssid,
            @RequestParam(value = "candidate-value-list") String[] candidateValueLists) {

        // Check if password file is valid
        byte[] passwordCaptureFileBytes;
        try {
            passwordCaptureFileBytes = passwordCaptureFile.getBytes();
            serviceFunctions.testForValidCrack(passwordCaptureFileBytes, ssid);
        } catch (IOException e) {
            throw new KrakenException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        for (String candidateValueListName : candidateValueLists) {
            CandidateValueList candidateValueList = candidateValueListRepository.findByListName(candidateValueListName);
            if (!Objects.equals(candidateValueList.getListType(), RequestType.WPA.name()))
                throw new KrakenException(HttpStatus.BAD_REQUEST, "List with name " + candidateValueListName + "" +
                        "was not a WPA CandidateValueList");
        }

        // Create Password Request Object
        CrackRequest crackRequest = new CrackRequest(
                null,
                RequestType.WPA.name(),
                ProcessingStatus.PENDING.name(),
                ssid,
                Base64.getEncoder().encodeToString(passwordCaptureFileBytes),
                String.join(":", candidateValueLists),
                null);

        // Add them to repository
        crackRequestRepository.save(crackRequest);

        // Submit and call processing core on another thread
        executorService.submit(() -> processingCore.process(null,
                null,
                null));

        // Return a response
        return new ResponseEntity<>(crackRequest, new HttpHeaders(), HttpStatus.CREATED);
    }

    // Delete a Password Request
    @DeleteMapping
    public ResponseEntity<CrackRequest> deletePasswordRequest(@RequestParam Long id) {
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
    public List<CrackRequest> getPendingPasswordRequests() {
        return crackRequestRepository.getAllPending();
    }

    @GetMapping(value = "/get-completed-password-requests", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<CrackRequest> getCompletedPasswordRequests() {
        return crackRequestRepository.getAllCompleted();
    }

    @GetMapping(value = "/test")
    public void test() {
        crackRequestRepository.save(
                new CrackRequest(null,
                        RequestType.WPA.name(),
                        ProcessingStatus.PENDING.name(),
                        "wali",
                        "walipash",
                        "default_list.txt",
                        null));
        executorService.submit(() -> processingCore.process(null,
                null,
                null));

    }

}

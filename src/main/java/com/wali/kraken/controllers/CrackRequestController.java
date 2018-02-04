package com.wali.kraken.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wali.kraken.core.server.ProcessingCore;
import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.domain.core.CrackRequestDescriptor;
import com.wali.kraken.domain.enumerations.ProcessingStatus;
import com.wali.kraken.domain.enumerations.RequestType;
import com.wali.kraken.repositories.CandidateValueListRepository;
import com.wali.kraken.repositories.CrackRequestDescriptorRepository;
import com.wali.kraken.services.ServiceFunctions;
import com.wali.kraken.utils.KrakenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Profile("server")
@RestController
public class CrackRequestController {

    private ExecutorService executorService;
    private ProcessingCore processingCore;
    private ServiceFunctions serviceFunctions;
    private CrackRequestDescriptorRepository crackRequestDescriptorRepository;
    private CandidateValueListRepository candidateValueListRepository;
    private ObjectMapper mapper;

    @Autowired
    public CrackRequestController(ServiceFunctions serviceFunctions,
                                  ProcessingCore processingCore,
                                  CrackRequestDescriptorRepository crackRequestDescriptorRepository,
                                  CandidateValueListRepository candidateValueListRepository) {
        this.serviceFunctions = serviceFunctions;
        this.processingCore = processingCore;
        this.executorService = Executors.newSingleThreadExecutor();
        this.crackRequestDescriptorRepository = crackRequestDescriptorRepository;
        this.candidateValueListRepository = candidateValueListRepository;
        this.mapper = new ObjectMapper();
    }

    // Create WPA Password Request
    @PostMapping(value = "/crack-request/createWPARequest")
    public ResponseEntity<CrackRequestDescriptor> createWPARequest(
            @RequestPart(value = "packet-capture-file") MultipartFile passwordCaptureFile,
            @RequestParam(value = "ssid") String ssid,
            @RequestParam(value = "candidate-value-list") String[] candidateValueLists) {

        // Check if password file is valid
        byte[] passwordCaptureFileBytes;
        try {
            passwordCaptureFileBytes = passwordCaptureFile.getBytes();
            serviceFunctions.testForValidCrack(passwordCaptureFileBytes, "server-temp-folder",  ssid);
        } catch (IOException e) {
            throw new KrakenException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        for (String candidateValueListName : candidateValueLists) {
            CandidateValueList candidateValueList = candidateValueListRepository.findByListName(candidateValueListName);
            if(candidateValueList == null)
                throw new KrakenException(HttpStatus.BAD_REQUEST, "List with name " + candidateValueListName + " " +
                        "does not exist");
            if (!Objects.equals(candidateValueList.getListType(), RequestType.WPA))
                throw new KrakenException(HttpStatus.BAD_REQUEST, "List with name " + candidateValueListName + " " +
                        "was not a WPA CandidateValueList");
        }

        Map<String,String> serializedMap = new HashMap<>();
        serializedMap.put("SSID", ssid);
        String mapAsString;
        try { mapAsString = mapper.writeValueAsString(serializedMap); }
        catch (Exception e){ throw new KrakenException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize SSID");}

        // Create Password Request Object
        CrackRequestDescriptor crackRequestDescriptor = new CrackRequestDescriptor(
                null,
                RequestType.WPA,
                ProcessingStatus.PENDING,
                Base64.getEncoder().encodeToString(passwordCaptureFileBytes),
                String.join(":", candidateValueLists),
                mapAsString,
                null);

        // Add them to repository
        crackRequestDescriptorRepository.save(crackRequestDescriptor);

        // Submit and call processing core on another thread
        executorService.submit(() -> processingCore.process(null,
                null,
                null));

        // Return a response
        return new ResponseEntity<>(crackRequestDescriptor, new HttpHeaders(), HttpStatus.CREATED);
    }

    // Delete a Password Request
    @DeleteMapping
    public ResponseEntity<CrackRequestDescriptor> deletePasswordRequest(@RequestParam Long id) {
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

    @GetMapping(value = "/crack-request/get-pending-password-requests", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<CrackRequestDescriptor> getPendingPasswordRequests() {
        return crackRequestDescriptorRepository.getAllPending();
    }

    @GetMapping(value = "/crack-request/get-completed-password-requests", produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<CrackRequestDescriptor> getCompletedPasswordRequests() {
        return crackRequestDescriptorRepository.getAllCompleted();
    }

}

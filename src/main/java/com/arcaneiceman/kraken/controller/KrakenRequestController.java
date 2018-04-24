package com.arcaneiceman.kraken.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Wali on 4/1/2018.
 */
@RestController
@RequestMapping("/api/requests")
public class KrakenRequestController {

    private static Logger log = LoggerFactory.getLogger(KrakenRequestController.class);


    @PostMapping(value = "/wpa")
    public ResponseEntity<KrakenRequestController> createWPARequest(
            @RequestParam(value = "packet-capture-file") MultipartFile passwordCaptureFile,
            @RequestParam(value = "ssid") String ssid,
            @RequestParam(value = "candidate-value-list") String[] candidateValueLists) {

        // TODO : Check if password file is valid
        byte[] passwordCaptureFileBytes;
//        try {
//            passwordCaptureFileBytes = passwordCaptureFile.getBytes();
//            serviceFunctions.testForValidCrack(passwordCaptureFileBytes, "server-temp-folder",  ssid);
//        } catch (IOException e) {
//            throw new KrakenException(HttpStatus.BAD_REQUEST, e.getMessage());
//        }

        return null;

    }
}

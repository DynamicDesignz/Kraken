package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.controller.io.ActiveRequestIO;
import com.arcaneiceman.kraken.domain.ActiveRequest;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.ActiveRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by Wali on 4/1/2018.
 */
@RestController
@RequestMapping("/api")
@Secured(AuthoritiesConstants.CONSUMER)
public class ActiveRequestController {

    private static Logger log = LoggerFactory.getLogger(ActiveRequestController.class);

    private ActiveRequestService activeRequestService;

    public ActiveRequestController(ActiveRequestService activeRequestService) {
        this.activeRequestService = activeRequestService;
    }

    @PostMapping(value = "/active-requests/wpa")
    public ResponseEntity<ActiveRequest> createWPAActiveRequest(
            @RequestParam(value = "packet-capture-file") MultipartFile passwordCaptureFile,
            @RequestParam(value = "ssid") String ssid,
            @RequestParam(value = "candidate-value-list") String[] candidateValueLists) {
        log.debug("REST Request to create WPA Request");
        return ResponseEntity.created(null).body(
                activeRequestService.createWPAActiveRequest(passwordCaptureFile, ssid, candidateValueLists));
    }

    @GetMapping(value = "/active-requests/{id}")
    public ResponseEntity<ActiveRequest> getActiveRequest(@PathVariable Long id) {
        log.debug("REST Request to get Request : {}", id);
        return ResponseEntity.ok(activeRequestService.getActiveRequest(id));
    }

    @PostMapping(value = "/active-requests/{id}/get-job")
    public ResponseEntity<ActiveRequestIO.GetJob.Response> getJob(@PathVariable Long id) {
        log.debug("REST Request to get Job for Request : {}", id);
        return ResponseEntity.ok(activeRequestService.getJob(id));
    }

    @PostMapping(value = "/active-requests/{id}/report-job")
    public ResponseEntity<Void> reportJob(@PathVariable Long id,
                                          @RequestParam String jobId,
                                          @RequestParam Boolean success,
                                          @RequestParam(required = false) String password) {
        log.debug("REST Request to report Job {} for Request : {}", jobId, id);
        activeRequestService.reportJob(id, jobId, success, password);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/active-requests/{id}")
    public ResponseEntity deleteActiveRequest(@PathVariable Long id) {
        log.debug("REST Request to delete Request : {}", id);
        activeRequestService.retireActiveRequest(id);
        return ResponseEntity.ok().build();
    }


}

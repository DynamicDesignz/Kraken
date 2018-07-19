package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.RequestService;
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
public class RequestController {

    private static Logger log = LoggerFactory.getLogger(RequestController.class);

    private RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping(value = "/requests")
    public ResponseEntity<Request> create(
            @RequestBody RequestIO.Create.Request requestDTO,
            @RequestParam(value = "packet-capture-file", required = false) MultipartFile passwordCaptureFile,
            @RequestParam(value = "candidate-value-list") String[] candidateValueLists) {
        log.debug("REST Request to create WPA Request");
        return ResponseEntity.created(null).body(
                requestService.createRequest(requestDTO, passwordCaptureFile, candidateValueLists));
    }

    @GetMapping(value = "/requests/{id}")
    public ResponseEntity<Request> get(@PathVariable Long id) {
        log.debug("REST Request to get Request : {}", id);
        return ResponseEntity.ok(requestService.getActiveRequest(id));
    }

    @PostMapping(value = "/requests/{id}/get-job")
    public ResponseEntity<RequestIO.GetJob.Response> getJob(@PathVariable Long id) {
        log.debug("REST Request to get Job for Request : {}", id);
        return ResponseEntity.ok(requestService.getJob(id));
    }

    @PostMapping(value = "/requests/{id}/report-job")
    public ResponseEntity<Void> reportJob(@PathVariable Long id,
                                          @RequestParam String jobId,
                                          @RequestParam Boolean success,
                                          @RequestParam(required = false) String password) {
        log.debug("REST Request to report Job {} for Request : {}", jobId, id);
        requestService.reportJob(id, jobId, success, password);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/active-requests/{id}")
    public ResponseEntity deleteActiveRequest(@PathVariable Long id) {
        log.debug("REST Request to delete Request : {}", id);
        requestService.retireActiveRequest(id);
        return ResponseEntity.ok().build();
    }


}

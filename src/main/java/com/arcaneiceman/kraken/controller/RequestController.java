package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.controller.io.RequestIO;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.RequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
    public ResponseEntity<Request> createWPA(
            @RequestParam(value = "details") String unmarshalledRequestDTO,
            @RequestParam(value = "capture-file", required = false) MultipartFile passwordCaptureFile)
            throws IOException, URISyntaxException {
        log.debug("REST Request to create WPA Request");
        Request request = requestService.create(
                new ObjectMapper().readValue(unmarshalledRequestDTO, RequestIO.Create.Request.class),
                passwordCaptureFile);
        return ResponseEntity.created(new URI("/api/requests" + request.getId().toString())).body(request);
    }

    @GetMapping(value = "/requests/{id}")
    public ResponseEntity<Request> get(@PathVariable Long id) {
        log.debug("REST Request to get Request : {}", id);
        return ResponseEntity.ok(requestService.get(id));
    }

    @GetMapping(value = "/requests")
    public Page<Request> get(Pageable pageable) {
        log.debug("REST Request to get Requests");
        return requestService.get(pageable);
    }

    @PostMapping(value = "/requests/get-job")
    public ResponseEntity<RequestIO.GetJob.Response> getJob(HttpServletRequest httpServletRequest) {
        log.debug("REST Request to get Job");
        return ResponseEntity.ok(requestService.getJob(httpServletRequest));
    }

    @PostMapping(value = "/requests/report-job")
    public ResponseEntity<Void> reportJob(@Valid @RequestBody RequestIO.ReportJob.Request requestDTO,
                                          HttpServletRequest httpServletRequest) {
        log.debug("REST Request to report Job");
        requestService.reportJob(requestDTO, httpServletRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/requests/{id}")
    public ResponseEntity deleteActiveRequest(@PathVariable Long id) {
        log.debug("REST Request to delete Request : {}", id);
        requestService.retireRequest(id);
        return ResponseEntity.ok().build();
    }


}

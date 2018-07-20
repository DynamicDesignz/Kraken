package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.controller.io.WorkerIO;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Secured(AuthoritiesConstants.CONSUMER)
public class WorkerController {

    private static Logger log = LoggerFactory.getLogger(WorkerController.class);
    private WorkerService workerService;

    @PostMapping(value = "/worker/register")
    public ResponseEntity<Worker> register(@RequestBody WorkerIO.Create.Request requestDTO) {
        log.debug("REST Request to register Worker Request");
        return ResponseEntity.created(workerService.create(requestDTO));
    }


}

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

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping(value = "/worker/augment-token")
    public ResponseEntity<WorkerIO.Augment.Response> augmentToken(@RequestBody WorkerIO.Augment.Request requestDTO){
        log.debug("Rest Request to Augment Token For Worker Login");
        return ResponseEntity.ok(workerService.augmentToken(requestDTO));
    }

    @PostMapping(value = "/worker")
    public ResponseEntity<Worker> create(@RequestBody WorkerIO.Create.Request requestDTO) {
        log.debug("REST Request for Worker Create");
        return ResponseEntity.created(null).body(workerService.create(requestDTO));
    }

    @PostMapping(value = "/worker/heartbeat")
    public ResponseEntity<Void> register(@RequestBody WorkerIO.Heartbeat.Request requestDTO) {
        log.debug("REST Request for Worker Heartbeat");
        workerService.heartbeat(requestDTO);
        return ResponseEntity.ok().build();
    }


}

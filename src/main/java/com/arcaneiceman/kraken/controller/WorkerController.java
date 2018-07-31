package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.controller.io.WorkerIO;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

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
    public ResponseEntity<WorkerIO.Augment.Response> augmentToken(@RequestBody WorkerIO.Augment.Request requestDTO) {
        log.debug("Rest Request to Augment Token For Worker Login");
        return ResponseEntity.ok(workerService.augmentToken(requestDTO));
    }

    @PostMapping(value = "/worker")
    public ResponseEntity<Worker> create(@RequestBody WorkerIO.Create.Request requestDTO) throws URISyntaxException {
        log.debug("REST Request for Worker Create");
        Worker worker = workerService.create(requestDTO);
        return ResponseEntity.created(new URI("/api/worker")).body(worker);
    }

    @PostMapping(value = "/worker/heartbeat")
    public ResponseEntity<Void> heartbeat(HttpServletRequest httpServletRequest) {
        log.debug("REST Request for Worker Heartbeat");
        workerService.heartbeat(httpServletRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/worker")
    public Page<Worker> getWorkers(Pageable pageable){
        log.debug("REST Request to get Workers");
        return workerService.get(pageable);
    }

    @DeleteMapping(value = "/worker")
    public ResponseEntity<Void> delete(@RequestBody WorkerIO.Delete.Request requestDTO){
        log.debug("REST Request to delete Workers");
        workerService.delete(requestDTO);
        return ResponseEntity.ok().build();
    }

}

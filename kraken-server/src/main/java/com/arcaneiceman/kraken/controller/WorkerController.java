package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.controller.io.WorkerIO;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
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

    @PostMapping(value = "/worker")
    public ResponseEntity<Worker> create(@RequestBody WorkerIO.Create.Request requestDTO) throws URISyntaxException {
        log.debug("REST Request for Worker Create");
        Worker worker = workerService.create(requestDTO);
        return ResponseEntity.created(new URI("/api/worker")).body(worker);
    }

    @PostMapping(value = "/worker/augment-token")
    public ResponseEntity<WorkerIO.Augment.Response> augmentToken(@RequestParam String workerName,
                                                                  @RequestParam String workerType) {
        log.debug("Rest Request to Augment Token For Worker Login");
        return ResponseEntity.ok(workerService.augmentToken(workerName, WorkerType.valueOf(workerType)));
    }

    @PostMapping(value = "/worker/heartbeat")
    public ResponseEntity<Void> heartbeat(HttpServletRequest httpServletRequest) {
        log.debug("REST Request for Worker Heartbeat");
        workerService.heartbeat(httpServletRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/workers")
    public Page<Worker> getWorkers(Pageable pageable) {
        log.debug("REST Request to get Workers");
        return workerService.get(pageable);
    }

    @GetMapping(value = "/worker")
    public ResponseEntity<Worker> get(@RequestParam String workerName,
                                      @RequestParam String workerType) {
        log.debug("REST Request to get Worker with name {} and type {}", workerName, workerType);
        return ResponseEntity.ok(workerService.get(workerName, WorkerType.valueOf(workerType), true));
    }

    @DeleteMapping(value = "/worker")
    public ResponseEntity<Void> delete(@RequestParam String workerName,
                                       @RequestParam String workerType) {
        log.debug("REST Request to delete Workers");
        workerService.delete(workerName, WorkerType.valueOf(workerType));
        return ResponseEntity.ok().build();
    }

}

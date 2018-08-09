package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.domain.Result;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Secured(AuthoritiesConstants.CONSUMER)
public class ResultController {

    private static Logger log = LoggerFactory.getLogger(ResultController.class);
    private ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/results")
    Page<Result> get(Pageable pageable) {
        return resultService.get(pageable);
    }

    @GetMapping(value = "/results/{id}")
    public ResponseEntity<Result> get(@PathVariable Long id) {
        log.debug("REST Request to get Result : {}", id);
        return ResponseEntity.ok(resultService.get(id));
    }

    @DeleteMapping(value = "/results/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST Request to delete Result : {}", id);
        resultService.delete(id);
        return ResponseEntity.ok().build();
    }
}

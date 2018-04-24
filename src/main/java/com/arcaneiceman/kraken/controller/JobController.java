package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job")
@Secured(AuthoritiesConstants.CONSUMER)
public class JobController {

    @PostMapping("/job-complete")
    public void jobComplete(){

    }

}

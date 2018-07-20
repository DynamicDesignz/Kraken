package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.domain.PasswordList;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.PasswordListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

@Secured(AuthoritiesConstants.CONSUMER)
public class PasswordListController {

    private final Logger log = LoggerFactory.getLogger(PasswordListController.class);
    private PasswordListService passwordListService;

    public PasswordListController(PasswordListService passwordListService) {
        this.passwordListService = passwordListService;
    }

    Page<PasswordList> getCandidateValueLists(Pageable pageable) {
        log.debug("REST request to get Candidate Value Lists");
        return passwordListService.get(pageable);
    }
}

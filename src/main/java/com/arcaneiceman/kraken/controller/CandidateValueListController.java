package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.domain.CandidateValueList;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.CandidateValueListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;

@Secured(AuthoritiesConstants.CONSUMER)
public class CandidateValueListController {

    private final Logger log = LoggerFactory.getLogger(CandidateValueListController.class);
    private CandidateValueListService candidateValueListService;

    public CandidateValueListController(CandidateValueListService candidateValueListService) {
        this.candidateValueListService = candidateValueListService;
    }

    Page<CandidateValueList> getCandidateValueLists(Pageable pageable) {
        log.debug("REST request to get Candidate Value Lists");
        return candidateValueListService.get(pageable);
    }
}

package com.wali.kraken.controllers;

import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.repositories.CandidateValueListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("server")
@RestController
public class CandidateValueListController {

    private CandidateValueListRepository candidateValueListRepository;

    @Autowired
    public CandidateValueListController(CandidateValueListRepository candidateValueListRepository) {
        this.candidateValueListRepository = candidateValueListRepository;
    }

    @GetMapping("/candidate-value-list")
    public List<CandidateValueList> getAll(){
        return candidateValueListRepository.findAll();
    }
}

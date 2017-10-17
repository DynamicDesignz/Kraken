package com.wali.kraken.controllers;

import com.wali.kraken.repositories.CandidateValueListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController("/password-list")
public class CandidateValueListController {

    private CandidateValueListRepository candidateValueListRepository;

    @Autowired
    public CandidateValueListController(CandidateValueListRepository candidateValueListRepository) {
        this.candidateValueListRepository = candidateValueListRepository;
    }

//    @GetMapping
//    public List<PasswordList> getAll(){
//        return candidateValueListRepository.findAll();
//    }
}

package com.wali.kraken.controllers;

import com.wali.kraken.domain.PasswordList;
import com.wali.kraken.repositories.PasswordListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/password-list")
public class PasswordListController {

    private PasswordListRepository passwordListRepository;

    @Autowired
    public PasswordListController(PasswordListRepository passwordListRepository){
        this.passwordListRepository = passwordListRepository;
    }

//    @GetMapping
//    public List<PasswordList> getAll(){
//        return passwordListRepository.findAll();
//    }
}

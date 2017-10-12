package com.wali.kraken.services;

import com.wali.kraken.domain.PasswordList;
import com.wali.kraken.repositories.PasswordListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PasswordListManager {

    private PasswordListRepository passwordListRepository;

    @Autowired
    public PasswordListManager(PasswordListRepository passwordListRepository) {
        this.passwordListRepository = passwordListRepository;

        // Load default list
        ClassLoader classLoader = this.getClass().getClassLoader();
        String path = classLoader.getResource("passwordlists/default_list.txt").getPath();
        PasswordList defaultPasswordList = new PasswordList(path);
        passwordListRepository.save(defaultPasswordList);
    }

    // TODO : Add Scheduled Task to check if there are new password lists put

}
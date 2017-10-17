package com.wali.kraken.services;

import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.repositories.CandidateValueListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;


@Service
public class CandidateValueListManager {

    private CandidateValueListRepository candidateValueListRepository;

    @Autowired
    public CandidateValueListManager(Environment environment, CandidateValueListRepository candidateValueListRepository) {
        this.candidateValueListRepository = candidateValueListRepository;

        String baseDirectory = environment.getProperty("kraken.tmp-folder.base", "./kraken-temp-folder");
        // Load default list
        File defaultList = new File(baseDirectory + "/" + "candidate_value_lists/" + "default_list.txt");
        if (!defaultList.exists())
            return;
        else {
            CandidateValueList defaultWPAList = new CandidateValueList(defaultList.getAbsolutePath());
            candidateValueListRepository.save(defaultWPAList);
        }


//        ClassLoader classLoader = this.getClass().getClassLoader();
//        String path = classLoader.getResource("candidate_value_lists/default_list.txt").getPath();
//        path = path.replaceFirst("^/(.:/)", "$1");
//        PasswordList defaultPasswordList = new PasswordList(path);
//        candidateValueListRepository.save(defaultPasswordList);
    }

    // TODO : Add Scheduled Task to check if there are new password lists put

}
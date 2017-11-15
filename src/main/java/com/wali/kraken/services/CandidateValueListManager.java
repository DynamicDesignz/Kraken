package com.wali.kraken.services;

import com.wali.kraken.config.Constants;
import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.enumerations.RequestType;
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
        File defaultList = new File(baseDirectory +
                "/" + Constants.CANDIDATE_VALUE_LIST_DIRECTORY + "/"
                + "default-list.txt");
        if (defaultList.exists()) {
            CandidateValueList defaultWPAList = new CandidateValueList(defaultList.getAbsolutePath(), RequestType.WPA);
            candidateValueListRepository.save(defaultWPAList);
        }
    }

    // TODO : Add Scheduled Task to check if there are new password lists put

}
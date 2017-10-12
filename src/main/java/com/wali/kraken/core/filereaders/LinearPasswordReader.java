package com.wali.kraken.core.filereaders;

import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.domain.overwire.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
public class LinearPasswordReader implements PasswordListReader {

    private Map<String, Scanner> scannerMap;
    private

    @Autowired
    LinearPasswordReader(){
        scannerMap = new HashMap<>();
    }

    @Override
    public Job readCandidateValuesIntoJob(JobDescriptor jobDescriptor) {
        //if(!scannerMap.containsKey(jobDescriptor.getPasswordListDescriptor().get))




        return null;
    }
}

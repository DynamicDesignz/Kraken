package com.wali.kraken.core.filereaders;

import com.wali.kraken.domain.core.JobDescriptor;
import com.wali.kraken.domain.core.PasswordListDescriptor;
import com.wali.kraken.domain.overwire.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
public class LinearPasswordReader implements PasswordListReader {

    private Map<String, Scanner> scannerMap;

    @Autowired
    LinearPasswordReader(){
        scannerMap = new HashMap<>();
    }

//    public boolean initReader(PasswordListDescriptor passwordListDescriptor){
//        long rqn = passwordListDescriptor.getPasswordRequest().getQueueNumber();
//        long pldqn = passwordListDescriptor.getQueueNumber();
//        String key = rqn + "-" + pldqn;
//        if(scannerMap.containsKey(key))
//            return false;
//
//        scannerMap.put(key,new Scanner(passwordListDescriptor.getPasswordList().getListPath()));
//        return true;
//    }

    @Override
    public ArrayList<String> readCandidateValuesIntoJob(JobDescriptor jobDescriptor) {
        String key = jobDescriptor.getPasswordRequest().getQueueNumber()
                + "-" +
                jobDescriptor.getPasswordListDescriptor().getQueueNumber();
        if(!scannerMap.containsKey(key))
            scannerMap.put(key, new Scanner(jobDescriptor.getPasswordList().getListPath()));

        long range  = (jobDescriptor.getEnd() - jobDescriptor.getStartLine()) + 1;
        ArrayList<String> retVal = new ArrayList<>();
        for(long i=0; i < range; i++){
            try{retVal.add(scannerMap.get(key).nextLine());}
            catch (Exception e) {return null;}
        }

        if(!scannerMap.get(key).hasNext())
            scannerMap.get(key).close();
        return retVal;
    }
}

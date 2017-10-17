package com.wali.kraken.core.filereaders;

import com.wali.kraken.domain.core.JobDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
public class LinearPasswordReader implements PasswordListReader {

    private Map<String, Scanner> scannerMap;

    @Autowired
    LinearPasswordReader() {
        scannerMap = new HashMap<>();
    }

    @Override
    public ArrayList<String> readCandidateValuesIntoJob(JobDescriptor jobDescriptor) {
        String key = jobDescriptor.getPasswordRequest().getQueueNumber()
                + "-" +
                jobDescriptor.getCanditateValueListDescriptor().getQueueNumber();
        if (!scannerMap.containsKey(key))
            try {
                scannerMap.put(key, new Scanner(new File(jobDescriptor.getPasswordList().getListPath())));
            } catch (Exception e) {
                return null;
            }

        long range = (jobDescriptor.getEnd() - jobDescriptor.getStartLine()) + 1;
        ArrayList<String> retVal = new ArrayList<>();
        for (long i = 0; i < range; i++) {
            try {
                retVal.add(scannerMap.get(key).nextLine());
            } catch (Exception e) {
                return null;
            }
        }

        if (!scannerMap.get(key).hasNext())
            scannerMap.get(key).close();
        return retVal;
    }
}

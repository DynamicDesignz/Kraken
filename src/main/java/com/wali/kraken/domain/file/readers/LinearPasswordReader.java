package com.wali.kraken.domain.file.readers;

import com.wali.kraken.domain.core.JobDescriptor;
import com.wali.kraken.domain.file.readers.CandidateValueListReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class LinearPasswordReader implements CandidateValueListReader {

    private Scanner scanner;

    private long currentLine;

    public LinearPasswordReader(String candidateValueListPath) throws IOException{
        scanner = new Scanner(new File(candidateValueListPath));
    }

    @Override
    public ArrayList<String> readCandidateValuesIntoJob(JobDescriptor jobDescriptor) {
        // Ensure that the job is the next job in queue
        if(jobDescriptor.getStartLine() != currentLine)
            return null;

        ArrayList<String> retVal = new ArrayList<>();
        long range = (jobDescriptor.getEndLine() - jobDescriptor.getStartLine()) + 1;
        for (long i = 0; i < range; i++) {
            try { retVal.add(scanner.nextLine()); } catch (Exception e) { return null; }
            currentLine ++;
        }

        if (!scanner.hasNext())
            scanner.close();

        return retVal;
    }
}

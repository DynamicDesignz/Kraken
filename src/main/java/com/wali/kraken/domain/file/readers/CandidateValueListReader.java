package com.wali.kraken.domain.file.readers;

import com.wali.kraken.domain.core.JobDescriptor;

import java.util.ArrayList;

public interface CandidateValueListReader {

    public ArrayList<String> readCandidateValuesIntoJob(JobDescriptor jobDescriptor);

}

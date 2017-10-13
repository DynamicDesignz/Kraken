package com.wali.kraken.core.filereaders;

import com.wali.kraken.domain.core.JobDescriptor;
import com.wali.kraken.domain.overwire.Job;

import java.util.ArrayList;

public interface PasswordListReader {

    public ArrayList<String> readCandidateValuesIntoJob(JobDescriptor jobDescriptor);

}

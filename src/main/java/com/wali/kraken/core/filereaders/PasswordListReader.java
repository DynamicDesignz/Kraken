package com.wali.kraken.core.filereaders;

import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.domain.overwire.Job;

public interface PasswordListReader {

    public Job readCandidateValuesIntoJob(JobDescriptor jobDescriptor);

}

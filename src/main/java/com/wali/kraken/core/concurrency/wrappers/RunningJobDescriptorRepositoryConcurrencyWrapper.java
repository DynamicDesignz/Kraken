package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.repositories.jobdescriptors.RunningJobDescriptorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RunningJobDescriptorRepositoryConcurrencyWrapper {

    private RunningJobDescriptorRepository runningJobDescriptorRepository;
    private Logger log = LoggerFactory.getLogger(PendingJobDescriptorRepositoryConcurrencyWrapper.class);

    public RunningJobDescriptorRepositoryConcurrencyWrapper(
            RunningJobDescriptorRepository runningJobDescriptorRepository){
        this.runningJobDescriptorRepository = runningJobDescriptorRepository;
    }

    public synchronized long getCount(long requestQueueNumber, long passwordListQueueNumber){
        return runningJobDescriptorRepository
                .getCountOfJobDescriptorForRequestAndPasswordList(
                        requestQueueNumber, passwordListQueueNumber);
    }

    public synchronized long getCount(){
        return runningJobDescriptorRepository.count();
    }

    public synchronized JobDescriptor getOne(long id){
        return runningJobDescriptorRepository.getOne(id);
    }
}

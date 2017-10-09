package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.repositories.jobdescriptors.PendingJobDescriptorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PendingJobDescriptorRepositoryConcurrencyWrapper {

    private PendingJobDescriptorRepository pendingJobDescriptorRepository;
    private Logger log = LoggerFactory.getLogger(PendingJobDescriptorRepositoryConcurrencyWrapper.class);

    public PendingJobDescriptorRepositoryConcurrencyWrapper(
            PendingJobDescriptorRepository pendingJobDescriptorRepository){
        this.pendingJobDescriptorRepository = pendingJobDescriptorRepository;
    }

    /**
     * Synchronized Function
     *
     * @return next {@link JobDescriptor} or null if it does not exist
     */
    public synchronized JobDescriptor getNextJobDescriptorForRequestAndRemove(
            long requestQueueNumber, long passwordListQueueNumber){
        Page<JobDescriptor> jobDescriptorPage =
                pendingJobDescriptorRepository
                        .getFirstJobDescriptorForRequestAndPasswordList(
                                requestQueueNumber, passwordListQueueNumber, new PageRequest(0,1));

        // Check to ensure its just one
        JobDescriptor jobDescriptor;
        if(jobDescriptorPage.getTotalElements() == 0)
            return null;
        else if(jobDescriptorPage.getTotalElements() == 1)
            jobDescriptor = jobDescriptorPage.getContent().get(0);
        else{
            log.error("Fatal Error: JobDescriptor query returned more than one item");
            return null;
        }

        // If not null, then delete it from repo.
        if(jobDescriptor != null)
            pendingJobDescriptorRepository.delete(jobDescriptor);

        return jobDescriptor;
    }

    public synchronized long getCount(){
        return pendingJobDescriptorRepository.count();
    }

    public synchronized long getCount(long requestQueueNumber, long passwordListQueueNumber){
        return pendingJobDescriptorRepository
                .getCountOfJobDescriptorForRequestAndPasswordList(
                        requestQueueNumber, passwordListQueueNumber);
    }

    public synchronized void deleteCompletedJobDescriptor(JobDescriptor jobDescriptor){
        pendingJobDescriptorRepository.delete(jobDescriptor);
    }
}

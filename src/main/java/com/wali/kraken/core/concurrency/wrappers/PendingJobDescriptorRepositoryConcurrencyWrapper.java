package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.repositories.jobdescriptors.PendingJobDescriptorRepository;
import org.springframework.stereotype.Service;

@Service
public class PendingJobDescriptorRepositoryConcurrencyWrapper {

    private PendingJobDescriptorRepository pendingJobDescriptorRepository;

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
        JobDescriptor jobDescriptor =
                pendingJobDescriptorRepository
                        .getFirstJobDescriptorForRequestAndPasswordList(
                                requestQueueNumber, passwordListQueueNumber);

        // If not null, then delete it from repo.
        if(jobDescriptor != null)
            pendingJobDescriptorRepository.delete(jobDescriptor);

        return jobDescriptor;
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

package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.PasswordListDescriptor;
import com.wali.kraken.repositories.passwordlistdescriptors.PendingPasswordListDescriptorRepository;
import org.springframework.stereotype.Service;

@Service
public class PendingPasswordListDescriptorRepositoryConcurrencyWrapper {

    private PendingPasswordListDescriptorRepository pendingPasswordListDescriptorRepository;

    public PendingPasswordListDescriptorRepositoryConcurrencyWrapper(
            PendingPasswordListDescriptorRepository pendingPasswordListDescriptorRepository){
        this.pendingPasswordListDescriptorRepository = pendingPasswordListDescriptorRepository;
    }

    /**
     * Synchronized Function
     *
     * @return next {@link PasswordListDescriptor} or null if it does not exist
     */
    public synchronized PasswordListDescriptor getNextPasswordListDescriptorForRequestAndRemove(
            long requestQueueNumber){
        PasswordListDescriptor passwordListDescriptor =
                pendingPasswordListDescriptorRepository
                        .getFirstPasswordListDescriptorForRequest(requestQueueNumber);

        // If not null, then delete it from repo.
        if(passwordListDescriptor != null)
            pendingPasswordListDescriptorRepository.delete(passwordListDescriptor);

        return passwordListDescriptor;
    }

    public synchronized long getCountOfPasswordListDescriptorForRequest(
            long requestQueueNumber){
        return pendingPasswordListDescriptorRepository
                .getCountOfPasswordListDescriptorForRequest(requestQueueNumber);
    }

    public synchronized void deleteCompletedPasswordListDescriptor(
            PasswordListDescriptor passwordListDescriptor){
        pendingPasswordListDescriptorRepository.delete(passwordListDescriptor);
    }
}

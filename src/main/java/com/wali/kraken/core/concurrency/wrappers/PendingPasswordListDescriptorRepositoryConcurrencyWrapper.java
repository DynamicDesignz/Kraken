package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.PasswordListDescriptor;
import com.wali.kraken.repositories.passwordlistdescriptors.PendingPasswordListDescriptorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PendingPasswordListDescriptorRepositoryConcurrencyWrapper {

    private PendingPasswordListDescriptorRepository pendingPasswordListDescriptorRepository;
    private Logger log = LoggerFactory.getLogger(PendingPasswordListDescriptorRepositoryConcurrencyWrapper.class);

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
        Page<PasswordListDescriptor> passwordListDescriptorPage =
                pendingPasswordListDescriptorRepository
                        .getFirstPasswordListDescriptorForRequest(requestQueueNumber,
                                new PageRequest(0,1));

        // Check to ensure its just one
        PasswordListDescriptor passwordListDescriptor;
        if(passwordListDescriptorPage.getTotalElements() == 0)
            return null;
        else if(passwordListDescriptorPage.getTotalElements() == 1)
            passwordListDescriptor = passwordListDescriptorPage.getContent().get(0);
        else{
            log.error("Fatal Error: PasswordListDescriptor query returned more than one item");
            return null;
        }

        // If not null, then delete it from repo.
        if(passwordListDescriptor != null)
            pendingPasswordListDescriptorRepository.delete(passwordListDescriptor);

        return passwordListDescriptor;
    }

    public synchronized long getCount(long requestQueueNumber){
        return pendingPasswordListDescriptorRepository
                .getCountOfPasswordListDescriptorForRequest(requestQueueNumber);
    }

    public synchronized long getCount(){
        return pendingPasswordListDescriptorRepository.count();
    }

    public synchronized void deleteCompletedPasswordListDescriptor(
            PasswordListDescriptor passwordListDescriptor){
        pendingPasswordListDescriptorRepository.delete(passwordListDescriptor);
    }
}

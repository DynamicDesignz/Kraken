package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.passwordrequests.PendingPasswordRequestsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PendingPasswordRequestRepositoryConcurrencyWrapper {

    private PendingPasswordRequestsRepository pendingPasswordRequestRepository;
    private Logger log = LoggerFactory.getLogger(PendingPasswordRequestRepositoryConcurrencyWrapper.class);

    PendingPasswordRequestRepositoryConcurrencyWrapper(
            PendingPasswordRequestsRepository pendingPasswordRequestRepository){
        this.pendingPasswordRequestRepository = pendingPasswordRequestRepository;
    }

    /**
     * Synchronized Function
     *
     * @return next {@link PasswordRequest} or null if it does not exist
     */
    public synchronized PasswordRequest getNextPasswordRequestAndRemove(){
        Page<PasswordRequest> passwordRequestPage
                = pendingPasswordRequestRepository.getFirstPasswordRequest(new PageRequest(0,1));

        // Check to ensure its just one
        PasswordRequest passwordRequest;
        if(passwordRequestPage.getTotalElements() == 0)
            return null;
        else if(passwordRequestPage.getTotalElements() == 1)
            passwordRequest = passwordRequestPage.getContent().get(0);
        else{
            log.error("Fatal Error: PasswordRequest query returned more than one item");
            return null;
        }

        // If not null, then delete it from repo.
        if(passwordRequest != null)
            pendingPasswordRequestRepository.delete(passwordRequest);

        return passwordRequest;
    }

    public synchronized long getCount(){
        return pendingPasswordRequestRepository.count();
    }

    public synchronized void deleteCompletedPasswordRequest(PasswordRequest passwordRequest){
        pendingPasswordRequestRepository.delete(passwordRequest);
    }
}

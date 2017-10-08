package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.passwordrequests.PendingPasswordRequestsRepository;
import org.springframework.stereotype.Service;

@Service
public class PendingPasswordRequestRepositoryConcurrencyWrapper {

    private PendingPasswordRequestsRepository pendingPasswordRequestRepository;

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
        PasswordRequest passwordRequest = pendingPasswordRequestRepository.getFirstPasswordRequest();

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

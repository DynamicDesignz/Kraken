package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.passwordrequests.RunningPasswordRequestsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RunningPasswordRequestRepositoryConcurrencyWrapper {

    private RunningPasswordRequestsRepository runningPasswordRequestsRepository;
    private Logger log = LoggerFactory.getLogger(RunningPasswordRequestRepositoryConcurrencyWrapper.class);

    public RunningPasswordRequestRepositoryConcurrencyWrapper(
            RunningPasswordRequestsRepository runningPasswordRequestsRepository) {
        this.runningPasswordRequestsRepository = runningPasswordRequestsRepository;
    }

    public synchronized long getCount() {
        return runningPasswordRequestsRepository.count();
    }

    public synchronized PasswordRequest getOne(long id){
        return runningPasswordRequestsRepository.getOne(id);
    }

}

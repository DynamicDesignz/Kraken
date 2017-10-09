package com.wali.kraken.core.concurrency.wrappers;

import com.wali.kraken.domain.PasswordListDescriptor;
import com.wali.kraken.repositories.passwordlistdescriptors.RunningPasswordListDescriptorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RunningPasswordListDescriptorRepositoryConcurrencyWrapper {

    private RunningPasswordListDescriptorRepository runningPasswordListDescriptorRepository;
    private Logger log = LoggerFactory.getLogger(PendingPasswordRequestRepositoryConcurrencyWrapper.class);

    RunningPasswordListDescriptorRepositoryConcurrencyWrapper(
            RunningPasswordListDescriptorRepository runningPasswordListDescriptorRepository) {
        this.runningPasswordListDescriptorRepository = runningPasswordListDescriptorRepository;
    }

    public synchronized long getCount(long requestQueueNumber) {
        return runningPasswordListDescriptorRepository
                .getCountOfPasswordListDescriptorForRequest(requestQueueNumber);
    }

    public synchronized long getCount() {
        return runningPasswordListDescriptorRepository.count();
    }

    public synchronized PasswordListDescriptor getOne(long id) {
        return runningPasswordListDescriptorRepository.getOne(id);
    }
}

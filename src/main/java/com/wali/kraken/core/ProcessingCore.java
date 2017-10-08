package com.wali.kraken.core;

import com.wali.kraken.core.concurrency.wrappers.PendingJobDescriptorRepositoryConcurrencyWrapper;
import com.wali.kraken.core.concurrency.wrappers.PendingPasswordListDescriptorRepositoryConcurrencyWrapper;
import com.wali.kraken.core.concurrency.wrappers.PendingPasswordRequestRepositoryConcurrencyWrapper;
import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.domain.PasswordListDescriptor;
import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.jobdescriptors.CompletedJobDescriptorRepository;
import com.wali.kraken.repositories.jobdescriptors.RunningJobDescriptorRepository;
import com.wali.kraken.repositories.passwordlistdescriptors.CompletedPasswordListDescriptorRepository;
import com.wali.kraken.repositories.passwordlistdescriptors.RunningPasswordListDescriptorRepository;
import com.wali.kraken.repositories.passwordrequests.CompletedPasswordRequestsRepository;
import com.wali.kraken.repositories.passwordrequests.RunningPasswordRequestsRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProcessingCore {

    private int MAX_CONCURRENT_PASSWORD_REQUESTS;
    private int MAX_CONCURRENT_PASSWORD_LISTS;

    private PendingJobDescriptorRepositoryConcurrencyWrapper pendingJobs;
    private RunningJobDescriptorRepository runningJobs;
    private CompletedJobDescriptorRepository completedJobs;

    private PendingPasswordListDescriptorRepositoryConcurrencyWrapper pendingPasswordLists;
    private RunningPasswordListDescriptorRepository runningPasswordLists;
    private CompletedPasswordListDescriptorRepository completedPasswordLists;

    private PendingPasswordRequestRepositoryConcurrencyWrapper pendingRequests;
    private RunningPasswordRequestsRepository runningRequests;
    private CompletedPasswordRequestsRepository completedRequests;

    public ProcessingCore(
            Environment environment,
            PendingJobDescriptorRepositoryConcurrencyWrapper pendingJobs,
            RunningJobDescriptorRepository runningJobs,
            CompletedJobDescriptorRepository completedJobs,
            PendingPasswordListDescriptorRepositoryConcurrencyWrapper pendingPasswordLists,
            RunningPasswordListDescriptorRepository runningPasswordLists,
            CompletedPasswordListDescriptorRepository completedPasswordLists,
            PendingPasswordRequestRepositoryConcurrencyWrapper pendingRequests,
            RunningPasswordRequestsRepository runningRequests,
            CompletedPasswordRequestsRepository completedRequests) {
        this.pendingJobs = pendingJobs;
        this.runningJobs = runningJobs;
        this.completedJobs = completedJobs;

        this.pendingPasswordLists = pendingPasswordLists;
        this.runningPasswordLists = runningPasswordLists;
        this.completedPasswordLists =  completedPasswordLists;

        this.pendingRequests = pendingRequests;
        this.runningRequests = runningRequests;
        this.completedRequests = completedRequests;

        MAX_CONCURRENT_PASSWORD_REQUESTS = Integer.getInteger(
                environment.getProperty("kraken.concurrent.passwordrequests", "1"));
        MAX_CONCURRENT_PASSWORD_LISTS = Integer.getInteger(
                environment.getProperty("kraken.concurrent.passwordlists", "1"));
    }

    /**
     * Primary Core Function of Kraken
     *
     * All callbacks and password request initiations, and jobs submitted to Gearman
     * are handled by this function.
     *
     * Flow:
     *      Part 1:
     *
     *
     *
     * @param requestQueueNumber {@link PasswordRequest#queueNumber}
     * @param passwordListDescriptorQueueNumber {@link PasswordListDescriptor#queueNumber}
     * @param jobDescriptorQueueNumber {@link JobDescriptor#queueNumber}
     */
    public synchronized void process(Long requestQueueNumber,
                                     Long passwordListDescriptorQueueNumber,
                                     Long jobDescriptorQueueNumber){
        // Jobs
//        if(pendingJobs.getCount() == 0){
//
//        }


    }


}




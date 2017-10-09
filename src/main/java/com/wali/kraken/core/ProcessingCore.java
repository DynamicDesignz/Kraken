package com.wali.kraken.core;

import com.wali.kraken.core.concurrency.wrappers.*;
import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.domain.PasswordListDescriptor;
import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.jobdescriptors.CompletedJobDescriptorRepository;
import com.wali.kraken.repositories.passwordlistdescriptors.CompletedPasswordListDescriptorRepository;
import com.wali.kraken.repositories.passwordrequests.CompletedPasswordRequestsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ProcessingCore {

    private int MAX_CONCURRENT_PASSWORD_REQUESTS;
    private int MAX_CONCURRENT_PASSWORD_LISTS;
    private int JobNumberCount;

    private PendingJobDescriptorRepositoryConcurrencyWrapper pendingJobs;
    private RunningJobDescriptorRepositoryConcurrencyWrapper runningJobs;
    private CompletedJobDescriptorRepository completedJobs;

    private PendingPasswordListDescriptorRepositoryConcurrencyWrapper pendingPasswordLists;
    private RunningPasswordListDescriptorRepositoryConcurrencyWrapper runningPasswordLists;
    private CompletedPasswordListDescriptorRepository completedPasswordLists;

    private PendingPasswordRequestRepositoryConcurrencyWrapper pendingRequests;
    private RunningPasswordRequestRepositoryConcurrencyWrapper runningRequests;
    private CompletedPasswordRequestsRepository completedRequests;

    private Logger log = LoggerFactory.getLogger(ProcessingCore.class);

    @Autowired
    public ProcessingCore(
            Environment environment,
            PendingJobDescriptorRepositoryConcurrencyWrapper pendingJobs,
            RunningJobDescriptorRepositoryConcurrencyWrapper runningJobs,
            CompletedJobDescriptorRepository completedJobs,
            PendingPasswordListDescriptorRepositoryConcurrencyWrapper pendingPasswordLists,
            RunningPasswordListDescriptorRepositoryConcurrencyWrapper runningPasswordLists,
            CompletedPasswordListDescriptorRepository completedPasswordLists,
            PendingPasswordRequestRepositoryConcurrencyWrapper pendingRequests,
            RunningPasswordRequestRepositoryConcurrencyWrapper runningRequests,
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

        MAX_CONCURRENT_PASSWORD_REQUESTS = Integer.parseInt(
                environment.getProperty("kraken.concurrent.passwordrequests", "1"));
        MAX_CONCURRENT_PASSWORD_LISTS = Integer.parseInt(
                environment.getProperty("kraken.concurrent.passwordlists", "1"));
    }

    /**
     * Primary Core Function of Kraken
     *
     * All callbacks and password request initiations, and jobs submitted to Gearman
     * are handled by this function.
     *
     * Flow:
     *      Section 1:
     *
     *
     *
     * @param requestQueueNumber {@link PasswordRequest#queueNumber}
     * @param passwordListDescriptorQueueNumber {@link PasswordListDescriptor#queueNumber}
     * @param jobDescriptorQueueNumber {@link JobDescriptor#queueNumber}
     */
    @Async
    public synchronized void process(Long requestQueueNumber,
                                     Long passwordListDescriptorQueueNumber,
                                     Long jobDescriptorQueueNumber){
        // Section 1 : Jobs

        // Marking Jobs as Complete
        if(jobDescriptorQueueNumber != null){
            JobDescriptor jobDescriptor = runningJobs.getOne(jobDescriptorQueueNumber);
            if(jobDescriptor != null)
                markJobAsComplete(jobDescriptor);
            else{
                log.error("Needed to mark Job with id {} as complete but it was not found" +
                        "in the runningJobDescriptorRepository", jobDescriptorQueueNumber);
            }
        }

        // Marking PasswordLists as Complete
        if(requestQueueNumber != null && passwordListDescriptorQueueNumber != null &&
                pendingJobs.getCount(requestQueueNumber, passwordListDescriptorQueueNumber) == 0 &&
                runningJobs.getCount(requestQueueNumber, passwordListDescriptorQueueNumber) == 0) {

            PasswordListDescriptor passwordListDescriptor =
                    runningPasswordLists.getOne(passwordListDescriptorQueueNumber);
            if (passwordListDescriptor != null)
                markPasswordListAsComplete(passwordListDescriptor);
            else
                log.error("Needed to mark PasswordListDescriptor with id {} as complete " +
                                "but it was not found in the runningPasswordListRepository",
                        passwordListDescriptorQueueNumber);
        }

        // Sending New Jobs
        if(runningJobs.getCount() < JobNumberCount && pendingJobs.getCount() > 0){
            processNextJob();
            return;
        }

        // Section 2 : PasswordList

        // Marking Requests as Complete
        if(requestQueueNumber != null &&
                pendingPasswordLists.getCount(requestQueueNumber) == 0 &&
                runningPasswordLists.getCount(requestQueueNumber) == 0){

            PasswordRequest passwordRequest = runningRequests.getOne(requestQueueNumber);
            if(passwordRequest != null)
                markRequestAsComplete(passwordRequest);
            else
                log.error("Needed to mark PasswordRequest with id {} as complete" +
                        "but it was not found in the runningPasswordRequestRepository",
                        requestQueueNumber);

        }

        // Sending New PasswordLists
        if (runningPasswordLists.getCount() < MAX_CONCURRENT_PASSWORD_LISTS &&
                pendingPasswordLists.getCount() > 0){
            processNextPasswordList();
            return;
        }

        // Section 3 : Requests

        // Sending New PasswordRequests
        if (runningRequests.getCount() < MAX_CONCURRENT_PASSWORD_REQUESTS &&
                pendingRequests.getCount() > 0){
            processNextRequest();
            return;
        }
    }

    public void processNextRequest(PasswordRequest passwordRequest){

    }

    public void processNextPasswordList(PasswordListDescriptor passwordListDescriptor){};

    public void processNextJob(JobDescriptor jobDescriptor){};

    public void markJobAsComplete(JobDescriptor jobDescriptor){};

    public void recoverJob(JobDescriptor jobDescriptor){};

    public void markRequestAsComplete(PasswordRequest passwordRequest){};

    public void markPasswordListAsComplete(PasswordListDescriptor passwordListDescriptor){
    };

}




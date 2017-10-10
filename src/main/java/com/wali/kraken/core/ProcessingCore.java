package com.wali.kraken.core;

import com.wali.kraken.domain.JobDescriptor;
import com.wali.kraken.domain.PasswordListDescriptor;
import com.wali.kraken.domain.PasswordRequest;
import com.wali.kraken.repositories.jobdescriptors.CompletedJobDescriptorRepository;
import com.wali.kraken.repositories.jobdescriptors.PendingJobDescriptorRepository;
import com.wali.kraken.repositories.jobdescriptors.RunningJobDescriptorRepository;
import com.wali.kraken.repositories.passwordlistdescriptors.CompletedPasswordListDescriptorRepository;
import com.wali.kraken.repositories.passwordlistdescriptors.PendingPasswordListDescriptorRepository;
import com.wali.kraken.repositories.passwordlistdescriptors.RunningPasswordListDescriptorRepository;
import com.wali.kraken.repositories.passwordrequests.CompletedPasswordRequestsRepository;
import com.wali.kraken.repositories.passwordrequests.PendingPasswordRequestsRepository;
import com.wali.kraken.repositories.passwordrequests.RunningPasswordRequestsRepository;
import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProcessingCore {

    private int MAX_CONCURRENT_PASSWORD_REQUESTS;
    private int MAX_CONCURRENT_PASSWORD_LISTS;
    private int JobNumberCount;

    private PendingJobDescriptorRepository pendingJobs;
    private RunningJobDescriptorRepository runningJobs;
    private CompletedJobDescriptorRepository completedJobs;

    private PendingPasswordListDescriptorRepository pendingPasswordLists;
    private RunningPasswordListDescriptorRepository runningPasswordLists;
    private CompletedPasswordListDescriptorRepository completedPasswordLists;

    private PendingPasswordRequestsRepository pendingRequests;
    private RunningPasswordRequestsRepository runningRequests;
    private CompletedPasswordRequestsRepository completedRequests;

    private Logger log = LoggerFactory.getLogger(ProcessingCore.class);
    private ExecutorService executorService;
    private GearmanClient client;

    @Autowired
    public ProcessingCore(
            Environment environment,
            PendingJobDescriptorRepository pendingJobs,
            RunningJobDescriptorRepository runningJobs,
            CompletedJobDescriptorRepository completedJobs,
            PendingPasswordListDescriptorRepository pendingPasswordLists,
            RunningPasswordListDescriptorRepository runningPasswordLists,
            CompletedPasswordListDescriptorRepository completedPasswordLists,
            PendingPasswordRequestsRepository pendingRequests,
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

        MAX_CONCURRENT_PASSWORD_REQUESTS = Integer.parseInt(
                environment.getProperty("kraken.concurrent.passwordrequests", "1"));
        MAX_CONCURRENT_PASSWORD_LISTS = Integer.parseInt(
                environment.getProperty("kraken.concurrent.passwordlists", "1"));

        executorService = Executors.newSingleThreadExecutor();

        // Initialize Gearman Variables
        int gearmanServerPort = Integer.parseInt(
                environment.getProperty("gearman.server.port", "4730"));
        Gearman gearman = Gearman.createGearman();
        client = gearman.createGearmanClient();
        GearmanServer server = gearman.createGearmanServer("127.0.0.1", gearmanServerPort);
        client.addServer(server);
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
    public synchronized void process(Long requestQueueNumber,
                                     Long passwordListDescriptorQueueNumber,
                                     Long jobDescriptorQueueNumber){
        // Section 1 : Jobs

        // Marking Jobs as Complete
        if(jobDescriptorQueueNumber != null)
            markJobAsComplete(jobDescriptorQueueNumber);

        // Marking PasswordLists as Complete
        if(requestQueueNumber != null && passwordListDescriptorQueueNumber != null &&
                pendingJobs.getCount(requestQueueNumber, passwordListDescriptorQueueNumber) == 0 &&
                runningJobs.getCount(requestQueueNumber, passwordListDescriptorQueueNumber) == 0)
            markPasswordListAsComplete(passwordListDescriptorQueueNumber);

        // Sending New Jobs
        if(requestQueueNumber != null && passwordListDescriptorQueueNumber != null &&
                runningJobs.count() < JobNumberCount && pendingJobs.count() > 0){
            processNextJob();
            return;
        }

        // Section 2 : PasswordList

        // Marking Requests as Complete
        if(requestQueueNumber != null &&
                pendingPasswordLists.getCount(requestQueueNumber) == 0 &&
                runningPasswordLists.getCount(requestQueueNumber) == 0)
            markRequestAsComplete(requestQueueNumber);

        // Sending New PasswordLists
        if (requestQueueNumber != null &&
                runningPasswordLists.count() < MAX_CONCURRENT_PASSWORD_LISTS &&
                pendingPasswordLists.count() > 0){

            PasswordListDescriptor passwordListDescriptor = processNextPasswordList();
            // Async Recursive Dispatch
            executorService.execute(() -> process(
                    requestQueueNumber,
                    passwordListDescriptor.getQueueNumber(),
                    null));
            return;
        }

        // Section 3 : Requests

        // Sending New PasswordRequests
        if (runningRequests.count() < MAX_CONCURRENT_PASSWORD_REQUESTS &&
                pendingRequests.count() > 0){

            PasswordRequest passwordRequest = processNextRequest();
            // Async Recursive Dispatch
            executorService.execute(() -> process(
                    passwordRequest.getQueueNumber(),
                    null,
                    null));
            return;
        }
    }

    private PasswordRequest processNextRequest(){
        log.info("Attempting to process next password request...");
        Page<PasswordRequest> passwordRequestPage =
                pendingRequests.get(new PageRequest(0,1));
        PasswordRequest passwordRequest;
        if(passwordRequestPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a password request to process");
        else {
            passwordRequest = passwordRequestPage.getContent().get(0);
            pendingRequests.delete(passwordRequest);
        }
        // Create Password List entries

        log.info("Success! PasswordRequest {} added to running queue", passwordRequest);
        return passwordRequest;
    }

    private PasswordListDescriptor processNextPasswordList(){
        log.info("Attempting to process next password list...");
        Page<PasswordListDescriptor> passwordListDescriptorPage =
                pendingPasswordLists.get(new PageRequest(0,1));
        PasswordListDescriptor passwordListDescriptor;
        if(passwordListDescriptorPage.getTotalElements() < 1 )
            throw new RuntimeException("Failed to get a PasswordList to process");
        else {
            passwordListDescriptor = passwordListDescriptorPage.getContent().get(0);
            pendingPasswordLists.delete(passwordListDescriptor);
        }

        //createJobs
        //load reader

        // Set As Running
        runningPasswordLists.save(passwordListDescriptor);

        log.info("Success! PasswordList {} added to running queue", passwordListDescriptor);
        return passwordListDescriptor;
    }

    private void processNextJob(){
        log.info("Attempting to process next job...");
        Page<JobDescriptor> jobDescriptorPage = pendingJobs.get(new PageRequest(0, 1));
        JobDescriptor jobDescriptor;
        if(jobDescriptorPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a Job to process");
        else {
            jobDescriptor = jobDescriptorPage.getContent().get(0);
            pendingJobs.delete(jobDescriptor);
        }

//        Job job = new Job(jobDescriptor.getQueueNumber(),
//                jobDescriptor.getPasswordListDescriptor().getQueueNumber(),
//                jobDescriptor.getPasswordRequest().getQueueNumber(),
//                jobDescriptor.getPasswordRequest().get
//        )
//
//        // submit job
//        client.submitJob();

        runningJobs.save(jobDescriptor);

        log.info("Success! JobDescriptor {} added to running queue", jobDescriptor);
    }

    private void markJobAsComplete(long jobDescriptorQueueNumber){
        log.info("Marking Job with id {} as complete", jobDescriptorQueueNumber);
        JobDescriptor jobDescriptor = runningJobs.getOne(jobDescriptorQueueNumber);
        if(jobDescriptor == null)
            throw new RuntimeException("Could not find job in running queue");
        runningJobs.delete(jobDescriptor);
        completedJobs.save(jobDescriptor);
    }

    private void markPasswordListAsComplete(long passwordListDescriptorQueueNumber){
        log.info("Marking PasswordList with id {} as complete", passwordListDescriptorQueueNumber);
        PasswordListDescriptor passwordListDescriptor = runningPasswordLists.getOne(passwordListDescriptorQueueNumber);
        if(passwordListDescriptor == null)
            throw new RuntimeException("Could not find PasswordList in running queue");
        runningPasswordLists.delete(passwordListDescriptor);
        completedPasswordLists.save(passwordListDescriptor);
    }

    private void markRequestAsComplete(long requestQueueNumber){
        log.info("Marking Request with id {} as complete", requestQueueNumber);
        PasswordRequest passwordRequest = runningRequests.getOne(requestQueueNumber);
        if(passwordRequest == null)
            throw new RuntimeException("Could not find PasswordRequest in running queue");
        runningRequests.delete(passwordRequest);
        completedRequests.save(passwordRequest);
    }



    public void recoverJob(JobDescriptor jobDescriptor){}
}




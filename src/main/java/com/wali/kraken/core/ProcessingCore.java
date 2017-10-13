package com.wali.kraken.core;

import com.wali.kraken.core.filereaders.LinearPasswordReader;
import com.wali.kraken.domain.core.JobDescriptor;
import com.wali.kraken.domain.PasswordList;
import com.wali.kraken.domain.core.PasswordListDescriptor;
import com.wali.kraken.domain.core.PasswordRequest;
import com.wali.kraken.domain.overwire.Job;
import com.wali.kraken.enumerations.ProcessingStatus;
import com.wali.kraken.repositories.JobDescriptorRepository;
import com.wali.kraken.repositories.PasswordListDescriptorRepository;
import com.wali.kraken.repositories.PasswordListRepository;
import com.wali.kraken.repositories.PasswordRequestRepository;
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

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProcessingCore {

    private int MAX_CONCURRENT_PASSWORD_REQUESTS;
    private int MAX_CONCURRENT_PASSWORD_LISTS;
    private int MAXJobNumberCount = 1; // TODO REMOVE!!!!!

    private long JOB_SIZE;

    private JobDescriptorRepository jobDescriptorRepository;
    private PasswordListDescriptorRepository passwordListDescriptorRepository;
    private PasswordRequestRepository passwordRequestRepository;

    private PasswordListRepository passwordListRepository;

    private LinearPasswordReader linearPasswordReader;

    private Logger log = LoggerFactory.getLogger(ProcessingCore.class);
    private ExecutorService executorService;
    private GearmanClient client;

    @Autowired
    public ProcessingCore(
            Environment environment,
            LinearPasswordReader linearPasswordReader,
            PasswordListRepository passwordListRepository,
            JobDescriptorRepository jobDescriptorRepository,
            PasswordListDescriptorRepository passwordListDescriptorRepository,
            PasswordRequestRepository passwordRequestRepository) {
        this.linearPasswordReader = linearPasswordReader;

        this.jobDescriptorRepository = jobDescriptorRepository;
        this.passwordListDescriptorRepository = passwordListDescriptorRepository;
        this.passwordRequestRepository = passwordRequestRepository;

        this.passwordListRepository = passwordListRepository;

        MAX_CONCURRENT_PASSWORD_REQUESTS = Integer.parseInt(
                environment.getProperty("kraken.concurrent.passwordrequests", "1"));
        MAX_CONCURRENT_PASSWORD_LISTS = Integer.parseInt(
                environment.getProperty("kraken.concurrent.passwordlists", "1"));

        JOB_SIZE = Long.parseLong(
                environment.getProperty("kraken.core.job-size", "2000000"));

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
                jobDescriptorRepository.getPendingCountFor(requestQueueNumber, passwordListDescriptorQueueNumber) == 0 &&
                jobDescriptorRepository.getRunningCountFor(requestQueueNumber, passwordListDescriptorQueueNumber) == 0)
            markPasswordListAsComplete(passwordListDescriptorQueueNumber);

        // Sending New Jobs
        if(requestQueueNumber != null && passwordListDescriptorQueueNumber != null &&
                jobDescriptorRepository.getRunningCount() < MAXJobNumberCount &&
                jobDescriptorRepository.getPendingCount() > 0){
            JobDescriptor jobDescriptor = processNextJob();

            // If failed, call again to get next job
            if(jobDescriptor == null)
                // Async Recursive Dispatch
                executorService.execute(() -> process(
                        requestQueueNumber,
                        passwordListDescriptorQueueNumber,
                        null));
            return;
        }

        // Section 2 : PasswordList

        // Marking Requests as Complete
        if(requestQueueNumber != null &&
                passwordListDescriptorRepository.getPendingCountFor(requestQueueNumber) == 0 &&
                passwordListDescriptorRepository.getRunningCountFor(requestQueueNumber) == 0)
            markRequestAsComplete(requestQueueNumber);

        // Sending New PasswordLists
        if (requestQueueNumber != null &&
                passwordListDescriptorRepository.getRunningCount() < MAX_CONCURRENT_PASSWORD_LISTS &&
                passwordListDescriptorRepository.getPendingCount() > 0){

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
        if (passwordRequestRepository.getRunningCount() < MAX_CONCURRENT_PASSWORD_REQUESTS &&
                passwordRequestRepository.getPendingCount() > 0){

            PasswordRequest passwordRequest = processNextRequest();

            // If null, there was a processing error.
            if(passwordRequest == null)
                return;
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
                passwordRequestRepository.getFirstPendingRequest(new PageRequest(0,1));
        PasswordRequest passwordRequest;
        if(passwordRequestPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a password request to process");
        else
            passwordRequest = passwordRequestPage.getContent().get(0);

        // Create Password List entries
        String[] passwordLists = passwordRequest.getColonDelimitedPasswordListNames().split(":");
        for(String passwordListName : passwordLists){
            PasswordList passwordList = passwordListRepository.findByListName(passwordListName);
            if(passwordList == null){
                log.error("Failed to find list with name {}. Marking Request as ERROR!", passwordListName);
                passwordRequest.setProcessingStatus(ProcessingStatus.ERROR);
                passwordRequestRepository.save(passwordRequest);
                return null;
            }
            else{
                log.info("Added Password List with name {} as a descriptor for request number {}",
                        passwordListName, passwordRequest.getQueueNumber());
                passwordListDescriptorRepository.save(
                        new PasswordListDescriptor(null, ProcessingStatus.PENDING, passwordRequest, passwordList));
            }
        }
        passwordRequest.setProcessingStatus(ProcessingStatus.RUNNING);
        passwordRequestRepository.save(passwordRequest);
        log.info("Success! PasswordRequest {} added to running queue", passwordRequest);
        return passwordRequest;
    }

    private PasswordListDescriptor processNextPasswordList(){
        log.info("Attempting to process next password list...");
        Page<PasswordListDescriptor> passwordListDescriptorPage =
                passwordListDescriptorRepository.getFirstAvailablePending(new PageRequest(0,1));
        PasswordListDescriptor passwordListDescriptor;
        if(passwordListDescriptorPage.getTotalElements() < 1 )
            throw new RuntimeException("Failed to get a PasswordList to process");
        else
            passwordListDescriptor = passwordListDescriptorPage.getContent().get(0);

        // Create Jobs
        for(long i=0; i<passwordListDescriptor.getPasswordList().getLineCount(); i=i+JOB_SIZE){
            JobDescriptor jobDescriptor = new JobDescriptor(
                    null,
                    ProcessingStatus.PENDING,
                    passwordListDescriptor.getPasswordRequest(),
                    passwordListDescriptor,
                    passwordListDescriptor.getPasswordList(),
                    i+1,
                    Math.min(i+JOB_SIZE, passwordListDescriptor.getPasswordList().getLineCount()),
                    0);
            jobDescriptorRepository.save(jobDescriptor);
        }

        // Set As Running
        passwordListDescriptor.setProcessingStatus(ProcessingStatus.RUNNING);
        passwordListDescriptorRepository.save(passwordListDescriptor);

        log.info("Success! PasswordList {} added to running queue", passwordListDescriptor);
        return passwordListDescriptor;
    }

    private JobDescriptor processNextJob(){
        log.info("Attempting to process next job...");
        Page<JobDescriptor> jobDescriptorPage =
                jobDescriptorRepository.getFirstAvailableJob(new PageRequest(0, 1));
        JobDescriptor jobDescriptor;
        if(jobDescriptorPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a Job to process");
        else
            jobDescriptor = jobDescriptorPage.getContent().get(0);

        ArrayList<String> candidateValues = linearPasswordReader.readCandidateValuesIntoJob(jobDescriptor);
        if(candidateValues == null){
            log.error("Failed to get values from password list. Marking job as error");
            jobDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            jobDescriptorRepository.save(jobDescriptor);
            return null;
        }

        Job job = new Job(jobDescriptor.getQueueNumber(),
                jobDescriptor.getPasswordListDescriptor().getQueueNumber(),
                jobDescriptor.getPasswordRequest().getQueueNumber(),
                jobDescriptor.getPasswordRequest().getPasswordCaptureInBase64(),
                candidateValues,
                jobDescriptor.getPasswordListDescriptor().getPasswordList().getCharacterSet());
//
//        // submit job
//        client.submitJob();

        jobDescriptor.setProcessingStatus(ProcessingStatus.RUNNING);
        jobDescriptorRepository.save(jobDescriptor);
        log.info("Success! JobDescriptor {} added to running queue", jobDescriptor);
        return jobDescriptor;
    }

    private void markJobAsComplete(long jobDescriptorQueueNumber){
        log.info("Marking Job with id {} as complete", jobDescriptorQueueNumber);
        JobDescriptor jobDescriptor = jobDescriptorRepository.getOne(jobDescriptorQueueNumber);
        if(jobDescriptor == null)
            throw new RuntimeException("Could not find job!");
        if(jobDescriptor.getProcessingStatus() != ProcessingStatus.RUNNING)
            throw new RuntimeException("Job was not in running status");
        jobDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE);
        jobDescriptorRepository.save(jobDescriptor);
    }

    private void markPasswordListAsComplete(long passwordListDescriptorQueueNumber){
        log.info("Marking PasswordList with id {} as complete", passwordListDescriptorQueueNumber);
        PasswordListDescriptor passwordListDescriptor = passwordListDescriptorRepository.getOne(passwordListDescriptorQueueNumber);
        if(passwordListDescriptor == null)
            throw new RuntimeException("Could not find PasswordList!");
        if(passwordListDescriptor.getProcessingStatus() != ProcessingStatus.RUNNING)
            throw new RuntimeException("List was not in running status");
        passwordListDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE);
        passwordListDescriptorRepository.save(passwordListDescriptor);
    }

    private void markRequestAsComplete(long requestQueueNumber){
        log.info("Marking Request with id {} as complete", requestQueueNumber);
        PasswordRequest passwordRequest = passwordRequestRepository.getOne(requestQueueNumber);
        if(passwordRequest == null)
            throw new RuntimeException("Could not find PasswordRequest!");
        if(passwordRequest.getProcessingStatus() != ProcessingStatus.RUNNING)
            throw new RuntimeException("Request was not in running status");
        passwordRequest.setProcessingStatus(ProcessingStatus.COMPLETE);
        passwordRequestRepository.save(passwordRequest);
    }



    public void recoverJob(JobDescriptor jobDescriptor){}
}




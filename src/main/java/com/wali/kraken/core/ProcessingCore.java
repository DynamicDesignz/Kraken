package com.wali.kraken.core;

import com.wali.kraken.core.filereaders.LinearPasswordReader;
import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.domain.core.CandidateValueListDescriptor;
import com.wali.kraken.domain.core.CrackRequest;
import com.wali.kraken.domain.core.JobDescriptor;
import com.wali.kraken.domain.overwire.Job;
import com.wali.kraken.enumerations.ProcessingStatus;
import com.wali.kraken.repositories.CandidateValueListDescriptorRepository;
import com.wali.kraken.repositories.CandidateValueListRepository;
import com.wali.kraken.repositories.CrackRequestRepository;
import com.wali.kraken.repositories.JobDescriptorRepository;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProcessingCore {

    private int MAX_CONCURRENT_PASSWORD_REQUESTS;
    private int MAX_CONCURRENT_PASSWORD_LISTS;
    private int MAXJobNumberCount = 1; // TODO REMOVE!!!!!

    private long JOB_SIZE;

    private JobDescriptorRepository jobDescriptorRepository;
    private CandidateValueListDescriptorRepository candidateValueListDescriptorRepository;
    private CrackRequestRepository crackRequestRepository;

    private CandidateValueListRepository candidateValueListRepository;

    private LinearPasswordReader linearPasswordReader;

    private Logger log = LoggerFactory.getLogger(ProcessingCore.class);
    private ExecutorService executorService;
    private GearmanClient client;

    @Autowired
    public ProcessingCore(
            Environment environment,
            LinearPasswordReader linearPasswordReader,
            CandidateValueListRepository candidateValueListRepository,
            JobDescriptorRepository jobDescriptorRepository,
            CandidateValueListDescriptorRepository candidateValueListDescriptorRepository,
            CrackRequestRepository crackRequestRepository) {
        this.linearPasswordReader = linearPasswordReader;

        this.jobDescriptorRepository = jobDescriptorRepository;
        this.candidateValueListDescriptorRepository = candidateValueListDescriptorRepository;
        this.crackRequestRepository = crackRequestRepository;

        this.candidateValueListRepository = candidateValueListRepository;

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
     * <p>
     * All callbacks and password request initiations, and jobs submitted to Gearman
     * are handled by this function.
     * <p>
     * Flow:
     * Section 1:
     *
     * @param requestQueueNumber                {@link CrackRequest#queueNumber}
     * @param passwordListDescriptorQueueNumber {@link CandidateValueListDescriptor#queueNumber}
     * @param jobDescriptorQueueNumber          {@link JobDescriptor#queueNumber}
     */
    public synchronized void process(Long requestQueueNumber,
                                     Long passwordListDescriptorQueueNumber,
                                     Long jobDescriptorQueueNumber) {
        // Section 1 : Jobs

        // Marking Jobs as Complete
        if (jobDescriptorQueueNumber != null)
            markJobAsComplete(jobDescriptorQueueNumber);

        // Marking PasswordLists as Complete
        if (requestQueueNumber != null && passwordListDescriptorQueueNumber != null &&
                jobDescriptorRepository.getPendingCountFor(requestQueueNumber, passwordListDescriptorQueueNumber) == 0 &&
                jobDescriptorRepository.getRunningCountFor(requestQueueNumber, passwordListDescriptorQueueNumber) == 0)
            markPasswordListAsComplete(passwordListDescriptorQueueNumber);

        // Sending New Jobs
        if (requestQueueNumber != null && passwordListDescriptorQueueNumber != null &&
                jobDescriptorRepository.getRunningCount() < MAXJobNumberCount &&
                jobDescriptorRepository.getPendingCount() > 0) {
            JobDescriptor jobDescriptor = processNextJob();

            // If failed, call again to get next job
            if (jobDescriptor == null)
                // Async Recursive Dispatch
                executorService.execute(() -> process(
                        requestQueueNumber,
                        passwordListDescriptorQueueNumber,
                        null));
            return;
        }

        // Section 2 : PasswordList

        // Marking Requests as Complete
        if (requestQueueNumber != null &&
                candidateValueListDescriptorRepository.getPendingCountFor(requestQueueNumber) == 0 &&
                candidateValueListDescriptorRepository.getRunningCountFor(requestQueueNumber) == 0)
            markRequestAsComplete(requestQueueNumber);

        // Sending New PasswordLists
        if (requestQueueNumber != null &&
                candidateValueListDescriptorRepository.getRunningCount() < MAX_CONCURRENT_PASSWORD_LISTS &&
                candidateValueListDescriptorRepository.getPendingCount() > 0) {

            CandidateValueListDescriptor candidateValueListDescriptor = processNextPasswordList();
            // Async Recursive Dispatch
            executorService.execute(() -> process(
                    requestQueueNumber,
                    candidateValueListDescriptor.getQueueNumber(),
                    null));
            return;
        }

        // Section 3 : Requests

        // Sending New PasswordRequests
        if (crackRequestRepository.getRunningCount() < MAX_CONCURRENT_PASSWORD_REQUESTS &&
                crackRequestRepository.getPendingCount() > 0) {

            CrackRequest crackRequest = processNextRequest();

            // If null, there was a processing error.
            if (crackRequest == null)
                return;
            // Async Recursive Dispatch
            executorService.execute(() -> process(
                    crackRequest.getQueueNumber(),
                    null,
                    null));
            return;
        }
    }

    private CrackRequest processNextRequest() {
        log.info("Attempting to process next password request...");
        Page<CrackRequest> passwordRequestPage =
                crackRequestRepository.getFirstPendingRequest(new PageRequest(0, 1));
        CrackRequest crackRequest;
        if (passwordRequestPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a password request to process");
        else
            crackRequest = passwordRequestPage.getContent().get(0);

        // Create Password List entries
        String[] passwordLists = crackRequest.getColonDelimitedCandidateValueListNames().split(":");
        ArrayList<CandidateValueListDescriptor> candidateValueListDescriptors = new ArrayList<>();
        for (String passwordListName : passwordLists) {
            CandidateValueList candidateValueList = candidateValueListRepository.findByListName(passwordListName);
            if (candidateValueList == null) {
                log.error("Failed to find list with name {}. Marking Request as ERROR!", passwordListName);
                crackRequest.setProcessingStatus(ProcessingStatus.ERROR.name());
                crackRequestRepository.save(crackRequest);
                return null;
            } else {
                log.info("Added Password List with name {} as a descriptor for request number {}",
                        passwordListName, crackRequest.getQueueNumber());
                candidateValueListDescriptors.add(
                        new CandidateValueListDescriptor(null, ProcessingStatus.PENDING.name(), crackRequest, candidateValueList));
            }
        }
        // Save Together
        candidateValueListDescriptorRepository.save(candidateValueListDescriptors);
        crackRequest.setProcessingStatus(ProcessingStatus.RUNNING.name());
        crackRequestRepository.save(crackRequest);
        log.info("Success! PasswordRequest {} added to running queue", crackRequest);
        return crackRequest;
    }

    private CandidateValueListDescriptor processNextPasswordList() {
        log.info("Attempting to process next password list...");
        Page<CandidateValueListDescriptor> passwordListDescriptorPage =
                candidateValueListDescriptorRepository.getFirstAvailablePending(new PageRequest(0, 1));
        CandidateValueListDescriptor candidateValueListDescriptor;
        if (passwordListDescriptorPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a PasswordList to process");
        else
            candidateValueListDescriptor = passwordListDescriptorPage.getContent().get(0);

        // Create Jobs Descriptors
        for (long i = 0; i < candidateValueListDescriptor.getCandidateValueList().getLineCount(); i = i + JOB_SIZE) {
            JobDescriptor jobDescriptor = new JobDescriptor(
                    null,
                    ProcessingStatus.PENDING.name(),
                    candidateValueListDescriptor.getCrackRequest(),
                    candidateValueListDescriptor,
                    candidateValueListDescriptor.getCandidateValueList(),
                    i + 1,
                    Math.min(i + JOB_SIZE, candidateValueListDescriptor.getCandidateValueList().getLineCount()),
                    0);
            jobDescriptorRepository.save(jobDescriptor);
        }

        // Set As Running
        candidateValueListDescriptor.setProcessingStatus(ProcessingStatus.RUNNING.name());
        candidateValueListDescriptorRepository.save(candidateValueListDescriptor);

        log.info("Success! PasswordList {} added to running queue", candidateValueListDescriptor);
        return candidateValueListDescriptor;
    }

    private JobDescriptor processNextJob() {
        log.info("Attempting to process next job...");
        Page<JobDescriptor> jobDescriptorPage =
                jobDescriptorRepository.getFirstAvailableJob(new PageRequest(0, 1));
        JobDescriptor jobDescriptor;
        if (jobDescriptorPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a Job to process");
        else
            jobDescriptor = jobDescriptorPage.getContent().get(0);

        ArrayList<String> candidateValues = linearPasswordReader.readCandidateValuesIntoJob(jobDescriptor);
        if (candidateValues == null) {
            log.error("Failed to get values from password list. Marking job as error");
            jobDescriptor.setProcessingStatus(ProcessingStatus.ERROR.name());
            jobDescriptorRepository.save(jobDescriptor);
            return null;
        }

        Job job = new Job(jobDescriptor.getQueueNumber(),
                jobDescriptor.getCandidateValueListDescriptor().getQueueNumber(),
                jobDescriptor.getCrackRequest().getQueueNumber(),
                jobDescriptor.getCrackRequest().getPasswordCaptureInBase64(),
                candidateValues,
                jobDescriptor.getCandidateValueListDescriptor().getCandidateValueList().getCharacterSet());
//
//        // submit job
//        client.submitJob();

        jobDescriptor.setProcessingStatus(ProcessingStatus.RUNNING.name());
        jobDescriptorRepository.save(jobDescriptor);
        log.info("Success! JobDescriptor {} added to running queue", jobDescriptor);
        return jobDescriptor;
    }

    private void markJobAsComplete(long jobDescriptorQueueNumber) {
        log.info("Marking Job with id {} as complete", jobDescriptorQueueNumber);
        JobDescriptor jobDescriptor = jobDescriptorRepository.findOne(jobDescriptorQueueNumber);
        if (jobDescriptor == null)
            throw new RuntimeException("Could not find job!");
        if (!Objects.equals(jobDescriptor.getProcessingStatus(), ProcessingStatus.RUNNING.name()))
            throw new RuntimeException("Job was not in running status");
        jobDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE.name());
        jobDescriptorRepository.save(jobDescriptor);
    }

    private void markPasswordListAsComplete(long passwordListDescriptorQueueNumber) {
        log.info("Marking PasswordList with id {} as complete", passwordListDescriptorQueueNumber);
        CandidateValueListDescriptor candidateValueListDescriptor = candidateValueListDescriptorRepository.findOne(passwordListDescriptorQueueNumber);
        if (candidateValueListDescriptor == null)
            throw new RuntimeException("Could not find PasswordList!");
        if (!Objects.equals(candidateValueListDescriptor.getProcessingStatus(), ProcessingStatus.RUNNING.name()))
            throw new RuntimeException("List was not in running status");
        candidateValueListDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE.name());
        candidateValueListDescriptorRepository.save(candidateValueListDescriptor);
    }

    private void markRequestAsComplete(long requestQueueNumber) {
        log.info("Marking Request with id {} as complete", requestQueueNumber);
        CrackRequest crackRequest = crackRequestRepository.findOne(requestQueueNumber);
        if (crackRequest == null)
            throw new RuntimeException("Could not find PasswordRequest!");
        if (!Objects.equals(crackRequest.getProcessingStatus(), ProcessingStatus.RUNNING.name()))
            throw new RuntimeException("Request was not in running status");
        crackRequest.setProcessingStatus(ProcessingStatus.COMPLETE.name());
        crackRequestRepository.save(crackRequest);
    }


    public void recoverJob(JobDescriptor jobDescriptor) {
    }
}




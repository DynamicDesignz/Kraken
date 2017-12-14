package com.wali.kraken.core.server;

import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.domain.core.CandidateValueListDescriptor;
import com.wali.kraken.domain.core.CrackRequestDescriptor;
import com.wali.kraken.domain.core.JobDescriptor;
import com.wali.kraken.domain.file.readers.CandidateValueListReader;
import com.wali.kraken.domain.file.readers.LinearPasswordReader;
import com.wali.kraken.domain.overwire.Job;
import com.wali.kraken.enumerations.ProcessingStatus;
import com.wali.kraken.repositories.*;
import com.wali.kraken.services.ServiceFunctions;
import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Profile("server")
@Service("ProcessingCore")
public class ProcessingCore {

    private int MAX_CONCURRENT_CRACK_REQUESTS;
    private int MAX_CONCURRENT_CANDIDATE_VALUE_LISTS;
    private int MAX_JOB_RETRIES;

    private long JOB_SIZE;

    private JobDescriptorRepository jobDescriptorRepository;
    private CandidateValueListDescriptorRepository candidateValueListDescriptorRepository;
    private CrackRequestDescriptorRepository crackRequestDescriptorRepository;
    private FileReaderRepository fileReaderRepository;

    private CandidateValueListRepository candidateValueListRepository;
    private WorkerRepository workerRepository;

    private Logger log = LoggerFactory.getLogger(ProcessingCore.class);
    private ServiceFunctions serviceFunctions;
    private ExecutorService executorService;
    private GearmanClient client;

    @Autowired
    public ProcessingCore(
            Environment environment,
            WorkerRepository workerRepository,
            CandidateValueListRepository candidateValueListRepository,
            JobDescriptorRepository jobDescriptorRepository,
            FileReaderRepository fileReaderRepository,
            CandidateValueListDescriptorRepository candidateValueListDescriptorRepository,
            CrackRequestDescriptorRepository crackRequestDescriptorRepository,
            ServiceFunctions serviceFunctions) throws UnknownHostException {

        this.jobDescriptorRepository = jobDescriptorRepository;
        this.candidateValueListDescriptorRepository = candidateValueListDescriptorRepository;
        this.crackRequestDescriptorRepository = crackRequestDescriptorRepository;
        this.fileReaderRepository = fileReaderRepository;

        this.candidateValueListRepository = candidateValueListRepository;
        this.workerRepository = workerRepository;
        this.serviceFunctions = serviceFunctions;

        MAX_CONCURRENT_CRACK_REQUESTS = Integer.parseInt(
                environment.getProperty("kraken.server.core.concurrent-crack-requests", "1"));
        MAX_CONCURRENT_CANDIDATE_VALUE_LISTS = Integer.parseInt(
                environment.getProperty("kraken.server.core.concurrent-candidate-value-lists", "1"));
        MAX_JOB_RETRIES = Integer.parseInt(
                environment.getProperty("kraken.server.core.job-retry", "3"));

        JOB_SIZE = Long.parseLong(
                environment.getProperty("kraken.server.core.job-size", "2000000"));

        executorService = Executors.newSingleThreadExecutor();

        // Initialize Gearman Server
        int gearmanServerPort = Integer.parseInt(
                environment.getProperty("gearman.server.port", "4730"));
        String gearmanServerHost = environment.getProperty("gearman.server.host", "127.0.0.1");
        Gearman gearman = Gearman.createGearman();
        client = gearman.createGearmanClient();
        GearmanServer server = gearman.createGearmanServer(gearmanServerHost, gearmanServerPort);
        client.addServer(server);
    }


    /**
     * Primary Core Function of Kraken
     * <p>
     * All callbacks and password request initiations, and jobs submitted to Gearman
     * are handled by this function.
     *
     * @param crackRequestQueueNumber                {@link CrackRequestDescriptor#queueNumber}
     * @param candidateValueListDescriptorQueueNumber {@link CandidateValueListDescriptor#queueNumber}
     * @param jobDescriptorQueueNumber          {@link JobDescriptor#queueNumber}
     */
    public synchronized void process(Long crackRequestQueueNumber,
                                     Long candidateValueListDescriptorQueueNumber,
                                     Long jobDescriptorQueueNumber) {
        // Section 1 : Jobs

        // Marking Jobs as Complete
        if (jobDescriptorQueueNumber != null)
            markJobAsComplete(jobDescriptorQueueNumber);

        // Marking CandidateValueList as Complete
        if (crackRequestQueueNumber != null && candidateValueListDescriptorQueueNumber != null &&
                jobDescriptorRepository.getPendingCountFor(crackRequestQueueNumber, candidateValueListDescriptorQueueNumber) == 0 &&
                jobDescriptorRepository.getRunningCountFor(crackRequestQueueNumber, candidateValueListDescriptorQueueNumber) == 0)
            markCandidateValueListAsComplete(candidateValueListDescriptorQueueNumber);

        // Sending New Jobs
        if (crackRequestQueueNumber != null && candidateValueListDescriptorQueueNumber != null &&
                jobDescriptorRepository.getRunningCount() < workerRepository.getWorkerCount() &&
                jobDescriptorRepository.getPendingCount() > 0) {

            JobDescriptor jobDescriptor = processNextJob();

            // If null, there was a processing error
            // Result : Recall for next job
            if (jobDescriptor == null)
                // Async Recursive Dispatch
                executorService.execute(() -> process(
                        crackRequestQueueNumber,
                        candidateValueListDescriptorQueueNumber,
                        null));

            // Job was successfully submitted. End loop. Next call will come when job is complete
            return;
        }

        // Section 2 : PasswordList

        // Marking Requests as Complete
        if (crackRequestQueueNumber != null &&
                candidateValueListDescriptorRepository.getPendingCountFor(crackRequestQueueNumber) == 0 &&
                candidateValueListDescriptorRepository.getRunningCountFor(crackRequestQueueNumber) == 0)
            markRequestAsComplete(crackRequestQueueNumber);

        // Sending New PasswordLists
        if (crackRequestQueueNumber != null &&
                candidateValueListDescriptorRepository.getRunningCount() < MAX_CONCURRENT_CANDIDATE_VALUE_LISTS &&
                candidateValueListDescriptorRepository.getPendingCount() > 0) {

            CandidateValueListDescriptor candidateValueListDescriptor = processNextPasswordList();

            // If null, there was a processing error
            // Result : Recall for next candidate value list
            if(candidateValueListDescriptor ==  null){
                executorService.execute(() -> process(
                        crackRequestQueueNumber,
                        null,
                        null));
                return;
            }

            // Async Recursive Dispatch
            executorService.execute(() -> process(
                    crackRequestQueueNumber,
                    candidateValueListDescriptor.getQueueNumber(),
                    null));
            return;
        }

        // Section 3 : Requests

        // Sending New PasswordRequests
        if (crackRequestDescriptorRepository.getRunningCount() < MAX_CONCURRENT_CRACK_REQUESTS &&
                crackRequestDescriptorRepository.getPendingCount() > 0) {

            CrackRequestDescriptor crackRequestDescriptor = processNextRequest();

            // If null, there was a processing error
            // Result : End Processing Loop
            if (crackRequestDescriptor == null)
                return;

            // Async Recursive Dispatch
            executorService.execute(() -> process(
                    crackRequestDescriptor.getQueueNumber(),
                    null,
                    null));
            return;
        }
    }

    private CrackRequestDescriptor processNextRequest() {
        log.info("Attempting to process next crack request...");

        // Get next pending crack request
        Page<CrackRequestDescriptor> passwordRequestPage =
                crackRequestDescriptorRepository.getFirstPendingRequest(new PageRequest(0, 1));
        CrackRequestDescriptor crackRequestDescriptor;
        if (passwordRequestPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a crack request to process");
        else
            crackRequestDescriptor = passwordRequestPage.getContent().get(0);

        // Create Password List entries
        String[] passwordLists = crackRequestDescriptor.getColonDelimitedCandidateValueListNames().split(":");
        ArrayList<CandidateValueListDescriptor> candidateValueListDescriptors = new ArrayList<>();
        for (String passwordListName : passwordLists) {
            // Create Descriptor
            CandidateValueListDescriptor candidateValueListDescriptor =
                    new CandidateValueListDescriptor(null,
                            ProcessingStatus.PENDING,
                            crackRequestDescriptor,
                            passwordListName);
            // Resolve Candidate List
            CandidateValueList candidateValueList = candidateValueListRepository.findByListName(passwordListName);
            if (candidateValueList == null) {
                log.error("Failed to find list with name {}. Marking Request as ERROR!", passwordListName);
                crackRequestDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            }

            log.info("Added Candidate Value List with name {} as a descriptor for Crack Request Descriptor {}",
                        passwordListName, crackRequestDescriptor.getQueueNumber());
            candidateValueListDescriptors.add(candidateValueListDescriptor);
        }

        // Check if there is at least 1 pending password list descriptor (if not return error
        if(candidateValueListDescriptors.stream().noneMatch(candidateValueListDescriptor ->
                candidateValueListDescriptor.getProcessingStatus() == ProcessingStatus.PENDING)){
            crackRequestDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            crackRequestDescriptorRepository.save(crackRequestDescriptor);
            return null;
        }

        // Save Candidate Value List Descriptor
        candidateValueListDescriptorRepository.save(candidateValueListDescriptors);

        // Set Processing Status of Crack Request to Running
        crackRequestDescriptor.setProcessingStatus(ProcessingStatus.RUNNING);

        // Save Crack Request
        crackRequestDescriptorRepository.save(crackRequestDescriptor);

        log.info("Success! Crack Request {} added to running queue", crackRequestDescriptor.getQueueNumber());
        return crackRequestDescriptor;
    }

    private CandidateValueListDescriptor processNextPasswordList() {
        log.info("Attempting to process next candidate value list descriptor...");

        // Get Next Candidate Value List Descriptor
        Page<CandidateValueListDescriptor> passwordListDescriptorPage =
                candidateValueListDescriptorRepository.getFirstAvailablePending(new PageRequest(0, 1));
        CandidateValueListDescriptor candidateValueListDescriptor;
        if (passwordListDescriptorPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a candidate value list to process");
        else
            candidateValueListDescriptor = passwordListDescriptorPage.getContent().get(0);

        // Get Candidate Value List referenced in the descriptor
        CandidateValueList candidateValueList =
                candidateValueListRepository.findByListName(candidateValueListDescriptor.getCandidateValueListName());
        if (candidateValueList == null) {
            log.error("Failed to find list with name {}. Marking Candidate Value List Descriptor as ERROR!",
                    candidateValueListDescriptor.getCandidateValueListName());
            candidateValueListDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            candidateValueListDescriptorRepository.save(candidateValueListDescriptor);
            return null;
        }

        // Create a List Reader
        CandidateValueListReader candidateValueListReader;
        try{
            candidateValueListReader = new LinearPasswordReader(candidateValueList.getListPath());
        }catch (Exception e){
            log.error("Failed to create list reader for list with name {}. Marking Candidate Value List Descriptor as ERROR!",
                    candidateValueListDescriptor.getCandidateValueListName());
            candidateValueListDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            candidateValueListDescriptorRepository.save(candidateValueListDescriptor);
            return null;
        }

        // Save List Reader is list reader repository
        String key = candidateValueListDescriptor.getCrackRequestDescriptor().getQueueNumber() +
                        "-" +
                candidateValueListDescriptor.getQueueNumber();
        fileReaderRepository.put(key, candidateValueListReader);

        // Create Jobs Descriptors
        ArrayList<JobDescriptor> jobList = new ArrayList<>();
        for (long i = 0; i < candidateValueList.getLineCount(); i = i + JOB_SIZE) {
            JobDescriptor jobDescriptor = new JobDescriptor(
                    null,
                    ProcessingStatus.PENDING,
                    candidateValueListDescriptor,
                    null,
                    i + 1,
                    Math.min(i + JOB_SIZE, candidateValueList.getLineCount()),
                    0,
                    0);
            jobList.add(jobDescriptor);
        }

        // Save Jobs
        jobDescriptorRepository.save(jobList);

        // Set As Candidate Value List Descriptor as Running
        candidateValueListDescriptor.setProcessingStatus(ProcessingStatus.RUNNING);
        candidateValueListDescriptorRepository.save(candidateValueListDescriptor);

        log.info("Success! Candidate Value List Descriptor {} for Crack Request {} added to running queue",
                candidateValueListDescriptor.getQueueNumber(),
                candidateValueListDescriptor.getCrackRequestDescriptor().getQueueNumber());
        return candidateValueListDescriptor;
    }

    private JobDescriptor processNextJob() {
        log.info("Attempting to process next job...");

        // Get Next Pending Job
        Page<JobDescriptor> jobDescriptorPage =
                jobDescriptorRepository.getFirstAvailableJob(new PageRequest(0, 1));
        JobDescriptor jobDescriptor;
        if (jobDescriptorPage.getTotalElements() < 1)
            throw new RuntimeException("Failed to get a Job to process");
        else
            jobDescriptor = jobDescriptorPage.getContent().get(0);

        // Get Reader
        String fileReaderKeys = jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getQueueNumber() +
                "-" +
                jobDescriptor.getCandidateValueListDescriptor().getQueueNumber();
        CandidateValueListReader reader = fileReaderRepository.get(fileReaderKeys);
        if(reader == null){
            log.error("Reader for this job doesn't exist! Marking Job Descriptor as ERROR!");
            jobDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            jobDescriptorRepository.save(jobDescriptor);
            return null;
        }

        // Get Candidate Values if there aren't any present
        if(jobDescriptor.getColonDelimitedCandidateValues() == null){
            ArrayList<String> candidateValues = reader.readCandidateValuesIntoJob(jobDescriptor);
            if (candidateValues == null) {
                log.error("Failed to get values from Candidate Value List. Marking Job Descriptor as ERROR!");
                jobDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
                jobDescriptorRepository.save(jobDescriptor);
                return null;
            }
            jobDescriptor.setColonDelimitedCandidateValues( String.join(":", candidateValues));
        }

        // Get Candidate Value List referenced in the descriptor
        CandidateValueList candidateValueList =
                candidateValueListRepository.findByListName(jobDescriptor.getCandidateValueListDescriptor().getCandidateValueListName());
        if (candidateValueList == null) {
            log.error("Failed to find list with name {}. Marking Job Descriptor as ERROR!",
                    jobDescriptor.getCandidateValueListDescriptor().getCandidateValueListName());
            jobDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            jobDescriptorRepository.save(jobDescriptor);
            return null;
        }

        // Create Job that goes over the wire
        Job job = new Job(jobDescriptor.getQueueNumber(),
                jobDescriptor.getCandidateValueListDescriptor().getQueueNumber(),
                jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getQueueNumber(),
                jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getPasswordCaptureInBase64(),
                jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getMetadataMap(),
                jobDescriptor.getColonDelimitedCandidateValues(),
                candidateValueList.getCharacterSet());

        // Generate Job Key
        String jobDescriptorKey = serviceFunctions.generateJobDescriptorKey(jobDescriptor);

        // Serialize Job
        byte[] jobAsBytes = serviceFunctions.serializeJob(job);
        if(jobAsBytes == null){
            log.error("Failed to serialize job with id {} into byte array", jobDescriptorKey);
            jobDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
            jobDescriptorRepository.save(jobDescriptor);
            return null;
        }

        // Submit job
        client.submitJob(
                jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getRequestType().name(),
                jobAsBytes,
                jobDescriptorKey,
                new JobCallBack(this, serviceFunctions));

        // Set Descriptor Status to running
        jobDescriptor.setProcessingStatus(ProcessingStatus.RUNNING);

        // Save Job Descriptor
        jobDescriptorRepository.save(jobDescriptor);

        log.info("Success! Job Descriptor with id {} added", jobDescriptorKey);
        return jobDescriptor;
    }

    private void markJobAsComplete(long jobDescriptorQueueNumber) {
        log.info("Marking Job with id {} as complete", jobDescriptorQueueNumber);

        // Find Job Descriptor
        JobDescriptor jobDescriptor = jobDescriptorRepository.findOne(jobDescriptorQueueNumber);
        if (jobDescriptor == null)
            throw new RuntimeException("Could not find job!");
        if(Objects.equals(jobDescriptor.getProcessingStatus(), ProcessingStatus.COMPLETE))
            return;
        if (!Objects.equals(jobDescriptor.getProcessingStatus(), ProcessingStatus.RUNNING))
            throw new RuntimeException("Job was not in running status");

        // Set Status as Complete
        jobDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE);

        // Remove Values (To Save Space)
        jobDescriptor.setColonDelimitedCandidateValues(null);

        // Save Job Descriptor
        jobDescriptorRepository.save(jobDescriptor);
    }

    private void markCandidateValueListAsComplete(long candidateValueListDescriptorQueueNumber) {
        log.info("Marking Candidate Value List Descriptor with id {} as complete", candidateValueListDescriptorQueueNumber);

        // Find
        CandidateValueListDescriptor candidateValueListDescriptor =
                candidateValueListDescriptorRepository.findOne(candidateValueListDescriptorQueueNumber);
        if (candidateValueListDescriptor == null)
            throw new RuntimeException("Could not find PasswordList!");
        if(Objects.equals(candidateValueListDescriptor.getProcessingStatus(), ProcessingStatus.COMPLETE))
            return;
        if (!Objects.equals(candidateValueListDescriptor.getProcessingStatus(), ProcessingStatus.RUNNING))
            throw new RuntimeException("List was not in running status");

        // Delete file reader
        String key = candidateValueListDescriptor.getCrackRequestDescriptor().getQueueNumber() +
                "-" +
                candidateValueListDescriptor.getQueueNumber();
        fileReaderRepository.delete(key);

        // Set Status as Complete
        candidateValueListDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE);

        // Save Descriptor
        candidateValueListDescriptorRepository.save(candidateValueListDescriptor);
    }

    private void markRequestAsComplete(long requestQueueNumber) {
        log.info("Marking Request with id {} as complete", requestQueueNumber);
        CrackRequestDescriptor crackRequestDescriptor = crackRequestDescriptorRepository.findOne(requestQueueNumber);
        if (crackRequestDescriptor == null)
            throw new RuntimeException("Could not find PasswordRequest!");
        if(Objects.equals(crackRequestDescriptor.getProcessingStatus(), ProcessingStatus.COMPLETE))
            return;
        if (!Objects.equals(crackRequestDescriptor.getProcessingStatus(), ProcessingStatus.RUNNING))
            throw new RuntimeException("Request was not in running status");

        // Set Status as Complete
        crackRequestDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE);

        // Save Descriptor
        crackRequestDescriptorRepository.save(crackRequestDescriptor);
    }

    public synchronized void recoverJob(String jobDescriptorKey) {
        log.info("Attempting to recover job with key {}...", jobDescriptorKey);

        long[] ids = serviceFunctions.getJobDescriptorFromKey(jobDescriptorKey);
        // Find Job Descriptor
        JobDescriptor jobDescriptor = jobDescriptorRepository.getJobDescriptorByKey(ids[0], ids[1], ids[2]);
        if(jobDescriptor == null)
            throw new RuntimeException("Job with id " + jobDescriptorKey + " marked for recovery was not found!");
        if(jobDescriptor.getProcessingStatus() != ProcessingStatus.RUNNING)
            throw new RuntimeException("Job with id " + jobDescriptorKey + " marked for recovery was not in running status");

        // Fail Job
        if(jobDescriptor.getAttempts() >= MAX_JOB_RETRIES)
            jobDescriptor.setProcessingStatus(ProcessingStatus.ERROR);
        // Recover Job
        else{
            jobDescriptor.setProcessingStatus(ProcessingStatus.PENDING);
            jobDescriptor.setAttempts(jobDescriptor.getAttempts() + 1);
        }

        // Save in Repo
        jobDescriptorRepository.save(jobDescriptor);

    }

    public synchronized void additionalWorker(){
        log.info("Attempting to add additional worker");
        try {
            processNextJob();
        }
        catch (RuntimeException e){
            log.info("No pending jobs to execute for additional worker");
        }
    }

    public synchronized void valueFound(String jobDescriptorKey, String value){
        log.info("Value found in Job with with key {}...", jobDescriptorKey);

        long[] ids = serviceFunctions.getJobDescriptorFromKey(jobDescriptorKey);

        CrackRequestDescriptor crackRequestDescriptor = crackRequestDescriptorRepository.findOne(ids[0]);
        if(crackRequestDescriptor == null)
            throw new RuntimeException("Found Value for a crack request that doesn't exist");

        // Mark All Non Complete JobDescriptors As Complete
        List<JobDescriptor> jobDescriptorList = jobDescriptorRepository.getAllNotCompleteForRequest(ids[0]);
        for(JobDescriptor jd : jobDescriptorList){
            jd.setColonDelimitedCandidateValues(null);
            jd.setProcessingStatus(ProcessingStatus.COMPLETE);
            jobDescriptorRepository.save(jd);
        }

        // Mark All Non Complete CandidateValueListDescriptors as Complete
        List<CandidateValueListDescriptor> candidateValueListDescriptorList =
                candidateValueListDescriptorRepository.getAllNotCompleteForRequest(ids[0]);
        for(CandidateValueListDescriptor candidateValueListDescriptor : candidateValueListDescriptorList){
            candidateValueListDescriptor.setProcessingStatus(ProcessingStatus.COMPLETE);
            candidateValueListDescriptorRepository.save(candidateValueListDescriptor);
        }

        // Set Value
        crackRequestDescriptor.setResult(value);
        crackRequestDescriptorRepository.save(crackRequestDescriptor);
    }


}




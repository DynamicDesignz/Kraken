package com.wali.kraken.core.server;

import com.wali.kraken.domain.Worker;
import com.wali.kraken.repositories.WorkerRepository;
import com.wali.kraken.services.ServiceFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Wali on 12/3/2017.
 */
@Service("WorkerManager")
@Profile("server")
@DependsOn({"ProcessingCore"})
public class WorkerManager {

    private volatile int workerCount;

    private ServiceFunctions serviceFunctions;

    private ExecutorService executorService;

    private ProcessingCore processingCore;

    private Logger log = LoggerFactory.getLogger(WorkerManager.class);

    private WorkerRepository workerRepository;

    private int gearmanServerPort;

    @Autowired
    public WorkerManager(ServiceFunctions serviceFunctions,
                         ProcessingCore processingCore,
                         WorkerRepository workerRepository,
                         Environment environment){
        this.workerRepository = workerRepository;
        this.serviceFunctions = serviceFunctions;
        this.executorService = Executors.newSingleThreadExecutor();
        this.processingCore = processingCore;
        this.gearmanServerPort = Integer.parseInt(
                environment.getProperty("kraken.server.gearman-server-port", "4730"));
    }

    public WorkerManager(int workerCount) {
        this.workerCount = workerCount;
    }

    @Scheduled(fixedDelay = 5000)
    public void getWorkerCount(){
        // Get Gearman Status
        String output = serviceFunctions.sendTextCommandToGearmanServer("127.0.0.1", gearmanServerPort,"status");

        // If no output, it means we have run into a fatal exception
        if(output == null)
            throw new RuntimeException("Gearman Server unexpectedly shutdown/unreachable");

        String[] workers = output.split("\t");

        int newWorkerCount = Integer.parseInt(workers[3]);
        int difference = newWorkerCount - workerCount;
        if( difference  > 0 ){
            log.info("Worker Count Increased to {}", newWorkerCount);
            workerRepository.save(new Worker());
            executorService.execute(() -> processingCore.additionalWorker());
        }
        else if ( difference < 0){
            log.info("Worker Count Decreased to {}", newWorkerCount);
            Page<Worker> workerPage = workerRepository.getFirstAvailableJob(new PageRequest(0,1));
            if (workerPage.getTotalElements() < 1)
                log.error("Attempted to remove worker but no worker entry was available to remove");
            else{
                Worker worker = workerPage.getContent().get(0);
                workerRepository.delete(worker);
            }
        }

        workerCount = newWorkerCount;
    }

}

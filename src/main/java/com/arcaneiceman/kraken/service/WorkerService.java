package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.WorkerIO;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.embedded.WorkerPK;
import com.arcaneiceman.kraken.domain.enumerations.WorkerStatus;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import com.arcaneiceman.kraken.repository.WorkerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;

@Service
@Transactional
public class WorkerService {

    private WorkerRepository workerRepository;
    private UserService userService;

    public WorkerService(WorkerRepository workerRepository, UserService userService) {
        this.workerRepository = workerRepository;
        this.userService = userService;
    }

    public Worker create(WorkerIO.Create.Request requestDTO) {
        User user = userService.getUserOrThrow();
        Worker worker = new Worker();
        worker.setId(new WorkerPK(requestDTO.getWorkerName(), requestDTO.getWorkerType(), user.getId()));
        worker.setStatus(WorkerStatus.OFFLINE);
        return workerRepository.save(worker);
    }

    public Worker get(String workerName, WorkerType workerType){
        User user = userService.getUserOrThrow();
        return workerRepository.getOne(new WorkerPK(workerName, workerType, user.getId()));
    }

    public void updateBenchmark(String workerName, WorkerType workerType, Integer benchmark){
        User user = userService.getUserOrThrow();
        Worker worker = workerRepository.getOne(new WorkerPK(workerName, workerType, user.getId()));
        worker.setBenchmark(benchmark);
        workerRepository.save(worker);
    }

    public void register(String workerName, WorkerType workerType, Long valuesPerSecond) {
        User user = userService.getUserOrThrow();
        Worker worker = workerRepository.getOne(new WorkerPK(workerName, workerType, user.getId()));
        worker.setStatus(WorkerStatus.ONLINE);
        worker.setLastValuesPerSecond(valuesPerSecond);
        worker.setLastCheckIn(new Date());
        workerRepository.save(worker);
    }

}

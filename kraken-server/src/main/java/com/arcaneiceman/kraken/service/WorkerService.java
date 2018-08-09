package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.WorkerIO;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.embedded.WorkerPK;
import com.arcaneiceman.kraken.domain.enumerations.WorkerStatus;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import com.arcaneiceman.kraken.repository.WorkerRepository;
import com.arcaneiceman.kraken.security.jwt.TokenProvider;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.zalando.problem.Status;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

import static com.arcaneiceman.kraken.config.Constants.WORKER_NAME;
import static com.arcaneiceman.kraken.config.Constants.WORKER_TYPE;

@Service
@Transactional
public class WorkerService {

    private WorkerRepository workerRepository;
    private final TokenProvider tokenProvider;
    private UserService userService;
    private JobService jobService;

    @Value("${application.worker-settings.worker-expiry-in-milliseconds}")
    private String workerExpiryTime;

    public WorkerService(WorkerRepository workerRepository,
                         TokenProvider tokenProvider,
                         UserService userService,
                         JobService jobService) {
        this.workerRepository = workerRepository;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.jobService = jobService;
    }

    @PostConstruct
    public void checkVariables() {
        if (workerExpiryTime == null || workerExpiryTime.isEmpty())
            throw new RuntimeException("Application Worker Service - " +
                    "Worker Expiry Time Not Specified");
    }

    public Worker create(WorkerIO.Create.Request requestDTO) {
        User user = userService.getUserOrThrow();
        if (workerRepository.existsById(new WorkerPK(requestDTO.getWorkerName(), requestDTO.getWorkerType(), user.getId())))
            throw new SystemException(3242, "Worker already exists", Status.BAD_REQUEST);
        Worker worker = new Worker();
        worker.setId(new WorkerPK(requestDTO.getWorkerName(), requestDTO.getWorkerType(), user.getId()));
        worker.setStatus(WorkerStatus.OFFLINE);
        return workerRepository.save(worker);
    }

    public WorkerIO.Augment.Response augmentToken(String workerName, WorkerType workerType) {
        get(workerName, workerType);
        User user = userService.getUserOrThrow();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String jwt = tokenProvider.createToken(
                user.getLogin(),
                authentication,
                workerName,
                workerType);
        return new WorkerIO.Augment.Response(jwt);
    }

    public Worker get(String workerName, WorkerType workerType) {
        return get(workerName, workerType, false);
    }

    public Worker get(String workerName, WorkerType workerType, boolean pullJob) {
        User user = userService.getUserOrThrow();
        Worker worker = workerRepository.findById(new WorkerPK(workerName, workerType, user.getId()))
                .orElseThrow(() -> new SystemException(452, "Worker with name " + workerName +
                        " and type " + workerType + " not found", Status.NOT_FOUND));
        if (pullJob && worker.getJob() != null) {
            String lazyLoader = worker.getJob().toString();
        }
        return worker;
    }

    public Worker get(HttpServletRequest httpServletRequest) {
        String workerName = (String) httpServletRequest.getAttribute(WORKER_NAME);
        WorkerType workerType = httpServletRequest.getAttribute(WORKER_TYPE) != null ?
                WorkerType.valueOf((String) httpServletRequest.getAttribute(WORKER_TYPE)) : null;
        return get(workerName, workerType);
    }

    public Page<Worker> get(Pageable pageable) {
        User user = userService.getUserOrThrow();
        return workerRepository.findByOwner(user.getId(), pageable);
    }

    public void heartbeat(HttpServletRequest httpServletRequest) {
        Worker worker = get(httpServletRequest);
        worker.setStatus(WorkerStatus.ONLINE);
        worker.setLastCheckIn(new Date());
        workerRepository.save(worker);
    }

    public void delete(String workerName, WorkerType workerType) {
        Worker worker = get(workerName, workerType);
        workerRepository.delete(worker);
    }

    // TODO : Make this a Quartz Job
    @Scheduled(initialDelay = 5000L, fixedRateString = "${application.worker-settings.worker-offline-task-rate-in-milliseconds}")
    public void expireWorker() {
        // lastCheckedIn < currentTime - expiryTime
        List<Worker> offlineWorkers = workerRepository.getByStatusAndLastCheckInBefore(WorkerStatus.ONLINE,
                new Date(new Date().getTime() - Long.parseLong(workerExpiryTime)));
        for (Worker worker : offlineWorkers) {
            worker.setStatus(WorkerStatus.OFFLINE);
            if (worker.getJob() != null) {
                worker.getJob().setWorker(null);
                worker.setJob(null);
                workerRepository.save(worker);
            }
            // TODO: Send Email to user that worker is offline
        }
    }
}

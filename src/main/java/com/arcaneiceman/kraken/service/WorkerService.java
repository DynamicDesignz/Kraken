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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.zalando.problem.Status;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.Date;

import static com.arcaneiceman.kraken.config.Constants.WORKER_NAME;
import static com.arcaneiceman.kraken.config.Constants.WORKER_TYPE;

@Service
@Transactional
public class WorkerService {

    private WorkerRepository workerRepository;
    private final TokenProvider tokenProvider;
    private UserService userService;

    public WorkerService(WorkerRepository workerRepository, TokenProvider tokenProvider, UserService userService) {
        this.workerRepository = workerRepository;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    public WorkerIO.Augment.Response augmentToken(WorkerIO.Augment.Request requestDTO) {
        User user = userService.getUserOrThrow();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!workerRepository.existsById(new WorkerPK(requestDTO.getWorkerName(), requestDTO.getWorkerType(), user.getId())))
            throw new SystemException(452, "Worker with name " + requestDTO.getWorkerName() +
                    " and type " + requestDTO.getWorkerType() + " not found", Status.NOT_FOUND);
        String jwt = tokenProvider.createToken(
                user.getLogin(),
                authentication,
                requestDTO.getWorkerName(),
                requestDTO.getWorkerType());
        return new WorkerIO.Augment.Response(jwt);
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

    public Worker get(String workerName, WorkerType workerType) {
        User user = userService.getUserOrThrow();
        return workerRepository.findById(new WorkerPK(workerName, workerType, user.getId()))
                .orElseThrow(() -> new SystemException(452, "Worker with name " + workerName +
                        " and type " + workerType + " not found", Status.NOT_FOUND));
    }

    public Worker get(HttpServletRequest httpServletRequest) {
        User user = userService.getUserOrThrow();
        String workerName = (String) httpServletRequest.getAttribute(WORKER_NAME);
        WorkerType workerType = httpServletRequest.getAttribute(WORKER_TYPE) != null ?
                WorkerType.valueOf((String) httpServletRequest.getAttribute(WORKER_TYPE)) : null;
        if (workerName == null || workerType == null)
            throw new SystemException(23, "Worker Details Not Found", Status.BAD_REQUEST);
        return workerRepository.findById(new WorkerPK(workerName, workerType, user.getId()))
                .orElseThrow(() -> new SystemException(452, "Worker with name " + workerName +
                        " and type " + workerType + " not found", Status.NOT_FOUND));
    }

//    public Page<Worker> get(Pageable pageable){
//        workerRepository.fin
//    }

    public void heartbeat(HttpServletRequest httpServletRequest) {
        User user = userService.getUserOrThrow();
        String workerName = (String) httpServletRequest.getAttribute(WORKER_NAME);
        WorkerType workerType = WorkerType.valueOf((String) httpServletRequest.getAttribute(WORKER_TYPE));
        Worker worker = workerRepository.findById(
                new WorkerPK(workerName, workerType, user.getId()))
                .orElseThrow(() -> new SystemException(452, "Worker with name " + workerName +
                        " and type " + workerType + " not found", Status.NOT_FOUND));
        worker.setStatus(WorkerStatus.ONLINE);
        worker.setLastCheckIn(new Date());
        workerRepository.save(worker);
    }

}

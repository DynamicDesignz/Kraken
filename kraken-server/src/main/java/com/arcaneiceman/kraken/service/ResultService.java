package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.domain.Result;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.arcaneiceman.kraken.repository.ResultRepository;
import com.arcaneiceman.kraken.service.permission.abs.ResultPermissionLayer;
import com.arcaneiceman.kraken.service.request.detail.RequestDetailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ResultService {

    private ResultRepository resultRepository;
    private RequestDetailService requestDetailService;
    private ResultPermissionLayer resultPermissionLayer;
    private UserService userService;

    public ResultService(ResultRepository resultRepository,
                         RequestDetailService requestDetailService,
                         ResultPermissionLayer resultPermissionLayer,
                         UserService userService) {
        this.resultRepository = resultRepository;
        this.requestDetailService = requestDetailService;
        this.resultPermissionLayer = resultPermissionLayer;
        this.userService = userService;
    }

    public Result create(RequestType requestType,
                         RequestDetail requestDetail,
                         String value,
                         int totalJobCount,
                         int errorJobCount,
                         int completeJobCount) {
        Result result = new Result(null, requestType, requestDetail, value, totalJobCount, errorJobCount, completeJobCount);
        User user = userService.getUserOrThrow();
        result.setOwner(user);
        return resultRepository.save(result);
    }

    public Page<Result> get(Pageable pageable) {
        User user = userService.getUserOrThrow();
        return resultRepository.findByOwner(pageable, user);
    }

    public Result get(Long id) {
        User user = userService.getUserOrThrow();
        Result result = resultPermissionLayer.getWithOwner(id, user);
        result.setRequestDetail(requestDetailService.get(result.getRequestType(), result.getRequestDetail().getId()));
        return result;
    }

    public void delete(Long id) {
        User user = userService.getUserOrThrow();
        Result result = resultPermissionLayer.getWithOwner(id, user);
        requestDetailService.delete(result.getRequestType(), result.getRequestDetail().getId());
    }

}

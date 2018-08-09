package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

@Service
public class RequestPermissionLayer extends MtoOPermissionLayer<Request, Long> {

    public RequestPermissionLayer(PagingAndSortingRepository<Request, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return new SystemException(2342, "Request with id " + aLong + " not found", Status.NOT_FOUND);
    }
}

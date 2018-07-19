package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.Request;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class RequestPermissionLayer extends MtoOPermissionLayer<Request, Long> {

    public RequestPermissionLayer(PagingAndSortingRepository<Request, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return null;
    }
}

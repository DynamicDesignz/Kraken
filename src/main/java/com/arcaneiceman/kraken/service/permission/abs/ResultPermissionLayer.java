package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.Result;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class ResultPermissionLayer extends MtoOPermissionLayer<Result, Long>{

    public ResultPermissionLayer(PagingAndSortingRepository<Result, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return null;
    }
}

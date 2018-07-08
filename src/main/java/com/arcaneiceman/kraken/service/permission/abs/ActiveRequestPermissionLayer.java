package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.ActiveRequest;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class ActiveRequestPermissionLayer extends MtoOPermissionLayer<ActiveRequest, Long> {

    public ActiveRequestPermissionLayer(PagingAndSortingRepository<ActiveRequest, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return null;
    }
}

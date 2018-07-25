package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.abs.TrackedList;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class TrackedListPermissionLayer extends MtoOPermissionLayer<TrackedList, Long>{

    public TrackedListPermissionLayer(PagingAndSortingRepository<TrackedList, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return null;
    }
}

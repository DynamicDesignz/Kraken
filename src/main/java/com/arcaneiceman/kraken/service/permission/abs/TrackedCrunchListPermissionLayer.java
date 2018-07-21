package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.TrackedCrunchList;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class TrackedCrunchListPermissionLayer extends MtoOPermissionLayer<TrackedCrunchList, Long> {

    public TrackedCrunchListPermissionLayer(PagingAndSortingRepository<TrackedCrunchList, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return null;
    }
}

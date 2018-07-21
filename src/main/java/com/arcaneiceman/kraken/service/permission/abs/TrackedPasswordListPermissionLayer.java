package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class TrackedPasswordListPermissionLayer extends MtoOPermissionLayer<TrackedPasswordList, Long> {

    public TrackedPasswordListPermissionLayer(PagingAndSortingRepository<TrackedPasswordList, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return null;
    }
}

package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.TrackedPasswordListJob;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class TrackedPasswordListJobPermissionLayer extends MtoOPermissionLayer<TrackedPasswordListJob, String> {

    public TrackedPasswordListJobPermissionLayer(PagingAndSortingRepository<TrackedPasswordListJob, String> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(String s) {
        return null;
    }
}

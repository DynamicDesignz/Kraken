package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.TrackedJob;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class TrackedJobPermissionLayer extends MtoOPermissionLayer<TrackedJob, String>{

    public TrackedJobPermissionLayer(PagingAndSortingRepository<TrackedJob, String> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(String s) {
        return null;
    }
}

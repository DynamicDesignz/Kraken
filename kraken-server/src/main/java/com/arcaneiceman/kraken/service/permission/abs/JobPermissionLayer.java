package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.Job;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class JobPermissionLayer extends MtoOPermissionLayer<Job, String> {

    public JobPermissionLayer(PagingAndSortingRepository<Job, String> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(String s) {
        return null;
    }
}

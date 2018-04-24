package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.KrakenRequest;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;
import org.zalando.problem.AbstractThrowableProblem;

@Service
public class CrackRequestPermissionLayer extends MtoOPermissionLayer<KrakenRequest, Long> {

    public CrackRequestPermissionLayer(PagingAndSortingRepository<KrakenRequest, Long> pagingAndSortingRepository) {
        super(pagingAndSortingRepository);
    }

    @Override
    public AbstractThrowableProblem getNotFoundThrowable(Long aLong) {
        return null;
    }
}

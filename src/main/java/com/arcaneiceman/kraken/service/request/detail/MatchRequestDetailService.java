package com.arcaneiceman.kraken.service.request.detail;

import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import com.arcaneiceman.kraken.repository.MatchRequestDetailRepository;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Status;

@Service
@Transactional
public class MatchRequestDetailService {

    private MatchRequestDetailRepository matchRequestDetailRepository;

    public MatchRequestDetailService(MatchRequestDetailRepository matchRequestDetailRepository) {
        this.matchRequestDetailRepository = matchRequestDetailRepository;
    }

    public MatchRequestDetail create(MatchRequestDetail matchRequestDetail) {
        // Clear any id if present
        matchRequestDetail.setId(null);

        if (matchRequestDetail.getValueToMatch() == null || matchRequestDetail.getValueToMatch().isEmpty())
            throw new SystemException(234, "Value to Match is null", Status.BAD_REQUEST);

        return matchRequestDetailRepository.save(matchRequestDetail);
    }

    @Transactional(readOnly = true)
    public MatchRequestDetail get(Long id) {
        return matchRequestDetailRepository.getOne(id);
    }

    public void delete(Long id) {
        matchRequestDetailRepository.deleteById(id);
    }
}

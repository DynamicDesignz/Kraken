package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRequestDetailRepository extends JpaRepository<MatchRequestDetail, Long> {
}

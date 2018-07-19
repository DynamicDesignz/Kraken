package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.WPARequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WPARequestDetailRepository extends JpaRepository<WPARequestDetail, Long> {
}

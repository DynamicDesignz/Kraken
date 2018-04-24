package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.KrakenRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KrakenRequestRepository extends JpaRepository<KrakenRequest, Long> {
}

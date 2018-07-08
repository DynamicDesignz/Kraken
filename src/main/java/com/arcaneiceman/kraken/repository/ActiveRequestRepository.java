package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.ActiveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActiveRequestRepository extends JpaRepository<ActiveRequest, Long> {
}

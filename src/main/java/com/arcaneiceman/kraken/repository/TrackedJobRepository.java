package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.TrackedJob;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.enumerations.TrackedJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedJobRepository extends JpaRepository<TrackedJob, String> {

    TrackedJob findFirstByOwnerAndStatus(Request k, TrackedJobStatus trackedJobStatus);
}

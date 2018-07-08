package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.ActiveRequest;
import com.arcaneiceman.kraken.domain.TrackedJob;
import com.arcaneiceman.kraken.domain.enumerations.TrackedJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedJobRepository extends JpaRepository<TrackedJob, String> {

    TrackedJob findFirstByOwnerAndStatus(ActiveRequest k, TrackedJobStatus trackedJobStatus);
}

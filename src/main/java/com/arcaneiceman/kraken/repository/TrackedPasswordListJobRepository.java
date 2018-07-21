package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import com.arcaneiceman.kraken.domain.TrackedPasswordListJob;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedPasswordListJobRepository extends JpaRepository<TrackedPasswordListJob, String> {

    TrackedPasswordListJob findFirstByOwnerAndStatus(TrackedPasswordList k, TrackingStatus trackingStatus);
}

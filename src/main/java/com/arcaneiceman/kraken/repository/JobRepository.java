package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Job;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByTrackingStatusAndSubmittedAtBefore(TrackingStatus trackingStatus, Date currentTimeMinusExpiry);
}

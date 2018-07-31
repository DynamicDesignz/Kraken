package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findBySubmittedAtBefore(Date currentTimeMinusExpiry);
}

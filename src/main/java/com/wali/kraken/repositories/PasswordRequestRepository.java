package com.wali.kraken.repositories;

import com.wali.kraken.domain.core.PasswordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by Wali on 10/12/2017.
 */
public interface PasswordRequestRepository extends JpaRepository<PasswordRequest, Long> {

    @Query("SELECT COUNT(p) FROM PasswordRequest p WHERE p.processingStatus = 'PENDING'")
    long getPendingCount();

    @Query("SELECT COUNT(pr) FROM PasswordRequest pr WHERE pr.processingStatus = 'RUNNING'")
    long getRunningCount();

    @Query("SELECT pr FROM PasswordRequest pr WHERE pr.processingStatus = 'PENDING'")
    Page<PasswordRequest> getFirstPendingRequest(Pageable page);

    @Query("SELECT pr FROM PasswordRequest pr WHERE pr.processingStatus = 'PENDING'")
    List<PasswordRequest> getAllPending();

    @Query("SELECT pr FROM PasswordRequest pr WHERE pr.processingStatus = 'COMPLETED'")
    List<PasswordRequest> getAllCompleted();
}

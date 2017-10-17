package com.wali.kraken.repositories;

import com.wali.kraken.domain.core.CrackRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by Wali on 10/12/2017.
 */
public interface CrackRequestRepository extends JpaRepository<CrackRequest, Long> {

    @Query("SELECT COUNT(r) FROM CrackRequest r WHERE r.processingStatus = 'PENDING'")
    long getPendingCount();

    @Query("SELECT COUNT(r) FROM CrackRequest r WHERE r.processingStatus = 'RUNNING'")
    long getRunningCount();

    @Query("SELECT r FROM CrackRequest r WHERE r.processingStatus = 'PENDING'")
    Page<CrackRequest> getFirstPendingRequest(Pageable page);

    @Query("SELECT r FROM CrackRequest r WHERE r.processingStatus = 'PENDING'")
    List<CrackRequest> getAllPending();

    @Query("SELECT r FROM CrackRequest r WHERE r.processingStatus = 'COMPLETED'")
    List<CrackRequest> getAllCompleted();
}

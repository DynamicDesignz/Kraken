package com.wali.kraken.repositories;

import com.wali.kraken.domain.core.CandidateValueListDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by Wali on 10/12/2017.
 */
public interface CandidateValueListDescriptorRepository extends JpaRepository<CandidateValueListDescriptor, Long> {

    @Query("SELECT COUNT(var) FROM CandidateValueListDescriptor var " +
            "WHERE var.processingStatus = 'PENDING' " +
            "AND var.crackRequestDescriptor.queueNumber = ?1 ")
    long getPendingCountFor(long requestQueueNumber);

    @Query("SELECT COUNT(var) FROM CandidateValueListDescriptor var " +
            "WHERE var.processingStatus = 'RUNNING' " +
            "AND var.crackRequestDescriptor.queueNumber = ?1 ")
    long getRunningCountFor(long requestQueueNumber);

    @Query("SELECT COUNT(var) FROM CandidateValueListDescriptor var WHERE var.processingStatus = 'PENDING'")
    long getPendingCount();

    @Query("SELECT COUNT(var) FROM CandidateValueListDescriptor var WHERE var.processingStatus = 'RUNNING'")
    long getRunningCount();

    @Query("SELECT var FROM CandidateValueListDescriptor var WHERE var.processingStatus = 'PENDING'")
    Page<CandidateValueListDescriptor> getFirstAvailablePending(Pageable page);

    @Query("SELECT var FROM CandidateValueListDescriptor var " +
            "WHERE var.crackRequestDescriptor.queueNumber = ?1 " +
            "AND var.processingStatus != 'COMPLETE'")
    List<CandidateValueListDescriptor> getAllNotCompleteForRequest(long crackRequestDescriptorQueueNumber);
}

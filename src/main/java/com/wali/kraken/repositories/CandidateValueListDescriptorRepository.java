package com.wali.kraken.repositories;

import com.wali.kraken.domain.core.CanditateValueListDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by Wali on 10/12/2017.
 */
public interface CandidateValueListDescriptorRepository extends JpaRepository<CanditateValueListDescriptor, Long> {

    @Query("SELECT COUNT(pld) FROM PasswordListDescriptor pld " +
            "WHERE pld.processingStatus = 'PENDING' " +
            "AND pld.passwordRequest.queueNumber = ?1 ")
    long getPendingCountFor(long requestQueueNumber);

    @Query("SELECT COUNT(pld) FROM PasswordListDescriptor pld " +
            "WHERE pld.processingStatus = 'RUNNING' " +
            "AND pld.passwordRequest.queueNumber = ?1 ")
    long getRunningCountFor(long requestQueueNumber);

    @Query("SELECT COUNT(pld) FROM PasswordListDescriptor pld WHERE pld.processingStatus = 'PENDING'")
    long getPendingCount();

    @Query("SELECT COUNT(pld) FROM PasswordListDescriptor pld WHERE pld.processingStatus = 'RUNNING'")
    long getRunningCount();

    @Query("SELECT pld FROM PasswordListDescriptor pld WHERE pld.processingStatus = 'PENDING'")
    Page<CanditateValueListDescriptor> getFirstAvailablePending(Pageable page);
}

package com.wali.kraken.repositories;

import com.wali.kraken.domain.core.JobDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by Wali on 10/12/2017.
 */
public interface JobDescriptorRepository extends JpaRepository<JobDescriptor, Long> {

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd " +
            "WHERE jd.processingStatus = 'PENDING' " +
            "AND jd.passwordRequest.queueNumber = ?1 " +
            "AND jd.passwordListDescriptor.queueNumber = ?2")
    long getPendingCountFor(long requestQueueNumber, long passwordListQueueNumber);

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd " +
            "WHERE jd.processingStatus = 'RUNNING' " +
            "AND jd.passwordRequest.queueNumber = ?1 " +
            "AND jd.passwordListDescriptor.queueNumber = ?2")
    long getRunningCountFor(long requestQueueNumber, long passwordListQueueNumber);

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd WHERE jd.processingStatus = 'PENDING'")
    long getPendingCount();

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd WHERE jd.processingStatus = 'RUNNING'")
    long getRunningCount();

    @Query("SELECT jd FROM JobDescriptor jd WHERE jd.processingStatus = 'PENDING'")
    Page<JobDescriptor> getFirstAvailableJob(Pageable page);
}

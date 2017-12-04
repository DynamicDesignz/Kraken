package com.wali.kraken.repositories;

import com.wali.kraken.domain.core.JobDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by Wali on 10/12/2017.
 */
public interface JobDescriptorRepository extends JpaRepository<JobDescriptor, Long> {

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd " +
            "WHERE jd.processingStatus = 'PENDING' " +
            "AND jd.crackRequest.queueNumber = ?1 " +
            "AND jd.candidateValueListDescriptor.queueNumber = ?2")
    long getPendingCountFor(long requestQueueNumber, long passwordListQueueNumber);

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd " +
            "WHERE jd.processingStatus = 'RUNNING' " +
            "AND jd.crackRequest.queueNumber = ?1 " +
            "AND jd.candidateValueListDescriptor.queueNumber = ?2")
    long getRunningCountFor(long requestQueueNumber, long passwordListQueueNumber);

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd WHERE jd.processingStatus = 'PENDING'")
    long getPendingCount();

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd WHERE jd.processingStatus = 'RUNNING'")
    long getRunningCount();

    @Query("SELECT jd FROM JobDescriptor jd WHERE jd.processingStatus = 'PENDING'")
    Page<JobDescriptor> getFirstAvailableJob(Pageable page);

    @Query("SELECT jd FROM JobDescriptor jd " +
            "WHERE jd.candidateValueListDescriptor.crackRequestDescriptor.queueNumber = ?1 " +
            "AND jd.candidateValueListDescriptor.queueNumber = ?2 " +
            "AND jd.queueNumber = ?3")
    JobDescriptor getJobDescriptorByKey(long crackRequestDescriptorQueueNumber,
                                        long candidateValueListDescriptorQueueNumber,
                                        long jobDescriptorQueueNumber);


    @Query("SELECT jf FROM JobDescriptor jd " +
            "WHERE jd.candidateValueListDescriptor.crackRequestDescriptor.queueNumber = ?1 " +
            "AND jd.processStatus != 'COMPLETE' ")
    List<JobDescriptor> getAllNotCompleteForRequest(long crackRequestDescriptorQueueNumber);
}

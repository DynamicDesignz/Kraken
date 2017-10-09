package com.wali.kraken.repositories.jobdescriptors;

import com.wali.kraken.domain.JobDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingJobDescriptorRepository
        extends JpaRepository<JobDescriptor, Long> {

    @Query("SELECT jd FROM JobDescriptor jd " +
            "WHERE jd.passwordRequest.queueNumber = ?1 " +
            "AND jd.passwordListDescriptor.queueNumber = ?2 ")
    Page<JobDescriptor> getFirstJobDescriptorForRequestAndPasswordList(
            @Param("rqn") long requestQueueNumber,
            @Param("plqn") long passwordListQueueNumber,
            Pageable page);

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd " +
            "WHERE jd.passwordRequest.queueNumber = ?1 " +
            "AND jd.passwordListDescriptor.queueNumber = ?2")
    long getCountOfJobDescriptorForRequestAndPasswordList(long requestQueueNumber,
                                                          long passwordListQueueNumber);
}

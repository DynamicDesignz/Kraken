package com.wali.kraken.repositories.jobdescriptors;

import com.wali.kraken.domain.JobDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RunningJobDescriptorRepository
        extends JpaRepository<JobDescriptor, Long> {

    @Query("SELECT COUNT(jd) FROM JobDescriptor jd " +
            "WHERE jd.passwordRequest.queueNumber = ?1 " +
            "AND jd.passwordListDescriptor.queueNumber = ?2")
    long getCount(long requestQueueNumber, long passwordListQueueNumber);
}

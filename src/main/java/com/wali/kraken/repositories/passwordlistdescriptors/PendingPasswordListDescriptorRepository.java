package com.wali.kraken.repositories.passwordlistdescriptors;

import com.wali.kraken.domain.PasswordListDescriptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingPasswordListDescriptorRepository
        extends JpaRepository<PasswordListDescriptor, Long> {

    @Query("SELECT pld FROM PasswordListDescriptor pld " +
            "WHERE pld.passwordRequest.queueNumber = ?rqn")
    PasswordListDescriptor getFirstPasswordListDescriptorForRequest(
            @Param("rqn") long requestQueueNumber, Pageable page);

    @Query("SELECT COUNT(pld) FROM PasswordListDescriptor pld " +
            "WHERE pld.passwordRequest.queueNumber = ?1")
    long getCountOfPasswordListDescriptorForRequest(long requestQueueNumber);
}

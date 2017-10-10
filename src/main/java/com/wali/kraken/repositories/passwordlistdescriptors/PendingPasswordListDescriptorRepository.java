package com.wali.kraken.repositories.passwordlistdescriptors;

import com.wali.kraken.domain.PasswordListDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PendingPasswordListDescriptorRepository
        extends JpaRepository<PasswordListDescriptor, Long> {

    @Query("SELECT pld FROM PasswordListDescriptor pld")
    Page<PasswordListDescriptor> get(Pageable page);

    @Query("SELECT COUNT(pld) FROM PasswordListDescriptor pld " +
            "WHERE pld.passwordRequest.queueNumber = ?1")
    long getCount(long requestQueueNumber);
}

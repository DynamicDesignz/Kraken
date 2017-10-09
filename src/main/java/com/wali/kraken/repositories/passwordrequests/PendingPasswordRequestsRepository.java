package com.wali.kraken.repositories.passwordrequests;

import com.wali.kraken.domain.PasswordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PendingPasswordRequestsRepository
        extends JpaRepository<PasswordRequest, Long> {

    @Query("SELECT pr FROM PasswordRequest pr")
    Page<PasswordRequest> getFirstPasswordRequest(Pageable page);
}

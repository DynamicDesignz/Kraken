package com.wali.kraken.repositories.passwordrequests;

import com.wali.kraken.domain.PasswordRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorPasswordRequestRepository
        extends JpaRepository<PasswordRequest, Long> {
}

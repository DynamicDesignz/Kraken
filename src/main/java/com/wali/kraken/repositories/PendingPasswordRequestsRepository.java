package com.wali.kraken.repositories;

import com.wali.kraken.domain.PasswordRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingPasswordRequestsRepository
        extends JpaRepository<PasswordRequest, String> {

}

package com.wali.kraken.repositories.passwordrequests;

import com.wali.kraken.domain.PasswordRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunningPasswordRequestsRepository
        extends JpaRepository<PasswordRequest, Long> {
}

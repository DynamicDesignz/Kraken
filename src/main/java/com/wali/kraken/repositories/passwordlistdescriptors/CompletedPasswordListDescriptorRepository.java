package com.wali.kraken.repositories.passwordlistdescriptors;

import com.wali.kraken.domain.PasswordListDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletedPasswordListDescriptorRepository extends
        JpaRepository<PasswordListDescriptor, Long> {
}

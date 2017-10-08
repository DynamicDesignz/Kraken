package com.wali.kraken.repositories.jobdescriptors;

import com.wali.kraken.domain.JobDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletedJobDescriptorRepository
        extends JpaRepository<JobDescriptor, Long> {
}

package com.wali.kraken.repositories;

import com.wali.kraken.domain.core.CrackRequestDescriptor;
import com.wali.kraken.domain.core.CrackRequestDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by Wali on 10/12/2017.
 */
public interface CrackRequestDescriptorRepository extends JpaRepository<CrackRequestDescriptor, Long> {

    @Query("SELECT COUNT(r) FROM CrackRequestDescriptor r WHERE r.processingStatus = 'PENDING'")
    long getPendingCount();

    @Query("SELECT COUNT(r) FROM CrackRequestDescriptor r WHERE r.processingStatus = 'RUNNING'")
    long getRunningCount();

    @Query("SELECT r FROM CrackRequestDescriptor r WHERE r.processingStatus = 'PENDING'")
    Page<CrackRequestDescriptor> getFirstPendingRequest(Pageable page);

    @Query("SELECT r FROM CrackRequestDescriptor r WHERE r.processingStatus = 'PENDING'")
    List<CrackRequestDescriptor> getAllPending();

    @Query("SELECT r FROM CrackRequestDescriptor r WHERE r.processingStatus = 'COMPLETE'")
    List<CrackRequestDescriptor> getAllCompleted();
}

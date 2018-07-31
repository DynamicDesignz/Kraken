package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.embedded.WorkerPK;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, WorkerPK> {

    List<Worker> getByLastCheckInBefore(Date currentTimeMinusExpiry);

    @Query("select worker from Worker worker where worker.id.userId = ?1")
    Page<Worker> findByOwner(Long userId, Pageable pageable);
}

package com.wali.kraken.repositories;

import com.wali.kraken.domain.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by Wali on 12/3/2017.
 */
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @Query("SELECT COUNT(worker) FROM Worker worker")
    int getWorkerCount();

    @Query("SELECT worker FROM Worker worker")
    Page<Worker> getFirstAvailableJob(Pageable page);
}

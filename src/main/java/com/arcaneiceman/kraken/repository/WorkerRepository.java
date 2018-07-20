package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Worker;
import com.arcaneiceman.kraken.domain.embedded.WorkerPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, WorkerPK> {
}

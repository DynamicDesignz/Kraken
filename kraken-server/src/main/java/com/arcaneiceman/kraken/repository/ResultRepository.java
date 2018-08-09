package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Result;
import com.arcaneiceman.kraken.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    Page<Result> findByOwner(Pageable pageable, User owner);
}

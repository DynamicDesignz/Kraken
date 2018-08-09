package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.abs.TrackedList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedListRepository extends JpaRepository<TrackedList, Long>{
}

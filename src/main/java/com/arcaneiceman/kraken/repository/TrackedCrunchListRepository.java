package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.TrackedCrunchList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedCrunchListRepository extends JpaRepository<TrackedCrunchList, Long> {
}

package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.TrackedPasswordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedPasswordListRepository extends JpaRepository<TrackedPasswordList, Long> {
}

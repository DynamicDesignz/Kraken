package com.arcaneiceman.kraken.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedPasswordList extends JpaRepository<TrackedPasswordList, Long> {
}

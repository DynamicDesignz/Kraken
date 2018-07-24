package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByOwner(User user);
}

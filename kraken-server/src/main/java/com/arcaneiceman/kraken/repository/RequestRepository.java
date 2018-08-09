package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    Page<Request> findByOwner(Pageable pageable, User user);

    List<Request> findByOwner(User user);
}

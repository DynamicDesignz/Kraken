package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findUserByEmail(String email);

    User findUserByResetKey(String resetKey);

    Page<User> findUsersByAuthority(String authority, Pageable pageable);

    User findUserByEmailAndAuthority(String email, String authority);

}

package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.Authority;
import com.arcaneiceman.kraken.domain.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    @Query("select u from User u where u.email = ?1")
    Optional<User> findUserByLogin(String login);

    Optional<User> findUserByEmail(String email);

    Optional<User> findUserByResetKey(String resetKey);

    List<User> findUserByAuthoritiesContaining(Set<Authority> authoritySet);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findUserWithAuthoritiesById(Long id);

    @EntityGraph(attributePaths = "authorities")
    @Query("select u from User u where u.email = ?1")
    Optional<User> findUserWithAuthoritiesByLogin(String login);

    Optional<User> findUserByEmailIgnoreCase(String email);

}

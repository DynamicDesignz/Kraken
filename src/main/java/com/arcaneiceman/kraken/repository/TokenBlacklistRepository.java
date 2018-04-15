package com.arcaneiceman.kraken.repository;

import com.arcaneiceman.kraken.domain.BlacklistToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Created by wali on 30/10/17.
 */
@Repository
public interface TokenBlacklistRepository extends JpaRepository<BlacklistToken, Long> {

    BlacklistToken findByTokenDigest(String token);

    List<BlacklistToken> findByCleanUpTimeBefore(Instant instant);
}

package com.arcaneiceman.kraken.security.blacklist.service;

/**
 * Created by Wali on 12/03/18.
 */

import com.arcaneiceman.kraken.domain.BlacklistToken;
import com.arcaneiceman.kraken.repository.TokenBlacklistRepository;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Created by wali on 26/09/17.
 *
 * Token Blacklist Cache temporarily holds blacklisted tokenFromSkretting digest.
 *
 * All tokens expire after being added to cache in their validity period
 *
 * If validity period = t and token was created at t1 then token expires in t1 + t.
 *
 * Thus, if the token is blacklisted as soon as it is created, it will stay in cache as long as it is valid.
 *
 */
@Service
public class TokenBlacklist {

    private Logger log = LoggerFactory.getLogger(TokenBlacklist.class);

    @Getter
    @Value("${application.security.jwt-token-validity-in-milliseconds}")
    private long tokenValidityInMilliseconds;

    @PostConstruct
    public void verifyVariables() {
        if (tokenValidityInMilliseconds == 0)
            throw new RuntimeException("No Token Validity Period Defined");
    }
    private TokenBlacklistRepository tokenBlacklistRepository;

    public TokenBlacklist(TokenBlacklistRepository tokenBlacklistRepository) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    public void addToBlackList(String tokenDigest){
        Instant timeToCleanUp = new Date().toInstant().plus(tokenValidityInMilliseconds, ChronoUnit.MILLIS);
        tokenBlacklistRepository.save(new BlacklistToken(null, tokenDigest, timeToCleanUp));
    }

    public boolean isInBlacklist(String tokenDigest){
        return tokenBlacklistRepository.findByTokenDigest(tokenDigest) != null;
    }

    @Scheduled(cron = "* 50 * * * ?")
    public void cleanupTokens() {
        log.debug("Running Cleanup Token Cron");
        List<BlacklistToken> tokensToClean = tokenBlacklistRepository.findByCleanUpTimeBefore(new Date().toInstant());
        tokensToClean.forEach(blacklistToken -> tokenBlacklistRepository.delete(blacklistToken));
        log.debug("Finish Cleanup Token Cron");
    }
}

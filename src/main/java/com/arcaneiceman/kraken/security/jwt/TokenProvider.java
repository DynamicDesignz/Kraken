package com.arcaneiceman.kraken.security.jwt;

import com.arcaneiceman.kraken.config.Constants;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import io.jsonwebtoken.*;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Token Provider aka Token Parser
 * <p>
 * This class validates tokens and parses them from strings to retrieve claims
 */
//@RefreshScope
@Component
public class TokenProvider {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    @Value("${application.security.jwt-token-signing-key}")
    private String secretKey;

    @Getter
    @Value("${application.security.jwt-token-validity-in-milliseconds}")
    private long tokenValidityInMilliseconds;

    @Autowired
    public TokenProvider() { }

    @PostConstruct
    public void verifyVariables() {
        if (secretKey == null)
            throw new RuntimeException("No Secret Key Defined!");
        if (tokenValidityInMilliseconds == 0)
            throw new RuntimeException("No Token Validity Period Defined");
    }

    public Claims getClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    public String createToken(String username,
                              String workerName,
                              WorkerType workerType,
                              Authentication authentication) {

        Map<String, Object> claims = new HashMap<>();

        // Summary Token Validity
        Date now = new Date();
        Date tokenValidity = new Date(now.getTime() + tokenValidityInMilliseconds);

        // Add Authority to claims
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(":"));
        claims.put(Constants.AUTHORITIES_KEY, authorities);

        // Worker Name
        if (workerName != null)
            claims.put(Constants.WORKER_NAME, workerName);

        // Worker Type
        if (workerType != null)
            claims.put(Constants.WORKER_TYPE, workerType);

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .setExpiration(tokenValidity)
                .setIssuedAt(now)
                .setSubject(username)
                .compact();
    }

    public String createToken(String username, Authentication authentication){
        return createToken(username, null, null, authentication);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            log.info("Invalid JWT signature.");
            log.trace("Invalid JWT signature trace: {}", e);
        } catch (MalformedJwtException e) {
            log.info("Invalid JWT token.");
            log.trace("Invalid JWT token trace: {}", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
            log.trace("Expired JWT token trace: {}", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token.");
            log.trace("Unsupported JWT token trace: {}", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
            log.trace("JWT token compact of handler are invalid trace: {}", e);
        }
        return false;
    }
}

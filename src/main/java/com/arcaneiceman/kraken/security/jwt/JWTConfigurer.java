package com.arcaneiceman.kraken.security.jwt;

import com.arcaneiceman.kraken.security.blacklist.service.TokenBlacklist;
import com.arcaneiceman.kraken.security.jwt.filters.JWTBlacklistFilter;
import com.arcaneiceman.kraken.security.jwt.filters.JWTValidateFilter;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 *  Security Configuration which defines and adds filters to all http requests
 */
public class JWTConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    private TokenProvider tokenProvider;
    private TokenBlacklist tokenBlacklist;

    public JWTConfigurer(TokenProvider tokenProvider, TokenBlacklist tokenBlacklist) {
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    /**
     * Adds security filters to all http requests
     *
     * Filter Order :
     *
     *      1) {@link JWTValidateFilter}
     *
     *      2) {@link JWTBlacklistFilter}
     **/
    @Override
    public void configure(HttpSecurity http) throws Exception {
        JWTValidateFilter validateFilter = new JWTValidateFilter(tokenProvider);
        JWTBlacklistFilter blacklistFilter = new JWTBlacklistFilter(tokenBlacklist);
        http.addFilterBefore(validateFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(blacklistFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

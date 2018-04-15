package com.arcaneiceman.kraken.security.jwt.filters;

import com.arcaneiceman.kraken.config.Constants;
import com.arcaneiceman.kraken.security.blacklist.service.TokenBlacklist;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Filter : {@link JWTBlacklistFilter}
 *
 * Filter Order : Second
 *
 * Checks if the  blacklist cache contains the token (token's digest to be more specific)
 *
 * If it exists:
 *      Throw {@link HttpServletResponse}.SC_UNAUTHORIZED error
 * Else:
 *      Allow pass through
 */
public class JWTBlacklistFilter extends GenericFilterBean {

    private TokenBlacklist tokenBlacklist;

    public JWTBlacklistFilter(TokenBlacklist tokenBlacklist) {
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        String blacklist_digest = (String) servletRequest.getAttribute(Constants.BLACKLIST_DIGEST_KEY);
        if (blacklist_digest != null && tokenBlacklist.isInBlacklist(blacklist_digest))
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED, "The token is blocked");
        else
            filterChain.doFilter(servletRequest, servletResponse);
    }

}

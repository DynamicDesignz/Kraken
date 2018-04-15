package com.arcaneiceman.kraken.security.jwt.filters;

import com.arcaneiceman.kraken.security.jwt.JWTConfigurer;
import com.arcaneiceman.kraken.security.jwt.TokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.arcaneiceman.kraken.config.Constants.AUTHORITIES_KEY;
import static com.arcaneiceman.kraken.config.Constants.BLACKLIST_DIGEST_KEY;


/**
 * Security Filter : {@link JWTValidateFilter}
 * <p>
 * Filter Order : First
 * <p>
 * If Authentication Token ('Authorization' header in HTTP Request) is valid:
 * <p>
 * 1) Installs Spring Security Principal (Adds role to the {@link SecurityContextHolder})
 * <p>
 * 2) Adds Token Blacklist Digest as a request attribute
 * <p>
 * 3) Adds Processed Token as a request attribute
 */
public class JWTValidateFilter extends GenericFilterBean {

    private TokenProvider tokenProvider;

    public JWTValidateFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwt = resolveToken(httpServletRequest);
        if (StringUtils.hasText(jwt) && this.tokenProvider.validateToken(jwt)) {
            Claims claims = tokenProvider.getClaims(jwt);

            // Install Spring Security Principal
            Collection<? extends GrantedAuthority> authorities =
                    Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(":"))
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
            User principal = new User(claims.getSubject(), "", authorities);
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(principal, jwt, authorities));

            // Add Token Blacklist Digest
            String createdAt = claims.getIssuedAt().toString();
            String username = claims.getSubject();
            servletRequest.setAttribute(BLACKLIST_DIGEST_KEY, DigestUtils.md5DigestAsHex((username + createdAt).getBytes()));
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JWTConfigurer.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }
}

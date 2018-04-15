package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.AccountIO;
import com.arcaneiceman.kraken.security.SecurityUtils;
import com.arcaneiceman.kraken.security.blacklist.service.TokenBlacklist;
import com.arcaneiceman.kraken.security.jwt.TokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import static com.arcaneiceman.kraken.config.Constants.BLACKLIST_DIGEST_KEY;

@Service
public class AccountService {

    private final AuthenticationManager authenticationManager;

    private final TokenProvider tokenProvider;

    private final TokenBlacklist tokenBlacklist;

    public AccountService(AuthenticationManager authenticationManager,
                          TokenProvider tokenProvider,
                          TokenBlacklist tokenBlacklist) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    public AccountIO.Authenticate.Response authenticate(AccountIO.Authenticate.Request requestDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(requestDTO.getLogin(), requestDTO.getPassword());
        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.createToken(requestDTO.getLogin(), authentication);
            return new AccountIO.Authenticate.Response(jwt);
        } catch (Exception e) {
            throw new RuntimeException("");
        }
    }

    public AccountIO.Refresh.Response refresh() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String jwt = tokenProvider.createToken(SecurityUtils.getCurrentUserLogin().toString(), authentication);
        return new AccountIO.Refresh.Response(jwt);
    }

    public void logout(HttpServletRequest httpServletRequest){
        String tokenDigest = (String) httpServletRequest.getAttribute(BLACKLIST_DIGEST_KEY);
        tokenBlacklist.addToBlackList(tokenDigest);
    }

}

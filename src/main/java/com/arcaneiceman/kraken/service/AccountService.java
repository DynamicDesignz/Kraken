package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.AccountIO;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import com.arcaneiceman.kraken.security.SecurityUtils;
import com.arcaneiceman.kraken.security.blacklist.service.TokenBlacklist;
import com.arcaneiceman.kraken.security.jwt.TokenProvider;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.zalando.problem.Status;

import javax.servlet.http.HttpServletRequest;

import static com.arcaneiceman.kraken.config.Constants.*;

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
            throw new SystemException(23, "Authentication Failure", Status.BAD_REQUEST);
        }
    }

    public AccountIO.AuthenticateWorker.Response authenticateWorker(AccountIO.AuthenticateWorker.Request requestDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(requestDTO.getLogin(), requestDTO.getPassword());
        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.createToken(requestDTO.getLogin(), authentication,
                    requestDTO.getWorkerName(), requestDTO.getWorkerType());
            return new AccountIO.AuthenticateWorker.Response(jwt);
        } catch (Exception e) {
            throw new SystemException(23, "Authentication Failure", Status.BAD_REQUEST);
        }
    }

    public AccountIO.Refresh.Response refresh(HttpServletRequest httpServletRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Get Worker Type and Name (Best Effort)
        WorkerType workerType = (WorkerType) httpServletRequest.getAttribute(WORKER_TYPE);
        String workerName = (String) httpServletRequest.getAttribute(WORKER_NAME);
        String jwt = tokenProvider.createToken(SecurityUtils.getCurrentUserLogin(), authentication, workerName, workerType);
        return new AccountIO.Refresh.Response(jwt);
    }

    public void logout(HttpServletRequest httpServletRequest) {
        String tokenDigest = (String) httpServletRequest.getAttribute(BLACKLIST_DIGEST_KEY);
        tokenBlacklist.addToBlackList(tokenDigest);
    }

}

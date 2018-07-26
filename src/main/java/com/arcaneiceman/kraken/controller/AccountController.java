package com.arcaneiceman.kraken.controller;

import com.arcaneiceman.kraken.controller.io.AccountIO;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.service.AccountService;
import com.arcaneiceman.kraken.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final UserService userService;
    private final AccountService accountService;

    public AccountController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @Validated
    @Secured(AuthoritiesConstants.CONSUMER)
    @PostMapping(value = "/register")
    public ResponseEntity<AccountIO.Register.Response> register(@Valid @RequestBody AccountIO.Register.Request requestDTO) {
        log.debug("REST request to register user account");
        return ResponseEntity.created(null).body(userService.createNewUser(requestDTO, false));
    }

    @Validated
    @PostMapping(value = "/authenticate")
    public ResponseEntity<AccountIO.Authenticate.Response> authenticate(
            @Valid @RequestBody AccountIO.Authenticate.Request requestDTO) {
        log.debug("REST request to authenticate user account");
        return ResponseEntity.ok(accountService.authenticate(requestDTO));
    }

    @Validated
    @PostMapping(value = "/refresh")
    @Secured({AuthoritiesConstants.CONSUMER, AuthoritiesConstants.ADMIN})
    public ResponseEntity<AccountIO.Refresh.Response> refresh(HttpServletRequest request) {
        log.debug("REST request to refresh token");
        return ResponseEntity.ok(accountService.refresh(request));
    }

    @Validated
    @PostMapping(value = "/logout")
    @Secured({AuthoritiesConstants.CONSUMER, AuthoritiesConstants.ADMIN})
    public ResponseEntity<Void> logout(HttpServletRequest httpServletRequest) {
        log.debug("REST request to logout");
        accountService.logout(httpServletRequest);
        return ResponseEntity.ok().build();
    }

    @Validated
    @PostMapping(value = "/reset_password/init")
    public ResponseEntity<Void> requestResetPassword(@Valid @RequestBody AccountIO.ResetPasswordInit.Request requestDTO) {
        log.debug("REST request to reset password init");
        userService.requestPasswordReset(requestDTO);
        return ResponseEntity.ok().build();
    }

    @Validated
    @PostMapping(value = "/reset_password/complete")
    public ResponseEntity<Void> completePasswordReset(@Valid @RequestBody AccountIO.ResetPasswordComplete.Request requestDTO) {
        log.debug("REST request to reset password init");
        userService.completePasswordReset(requestDTO);
        return ResponseEntity.ok().build();
    }

    @Validated
    @PostMapping(value = "/change_password")
    @Secured({AuthoritiesConstants.CONSUMER, AuthoritiesConstants.ADMIN})
    public ResponseEntity<Void> changePassword(@Valid @RequestBody AccountIO.ChangePassword.Request requestDTO) {
        log.debug("REST request to change password");
        userService.changePassword(requestDTO);
        return ResponseEntity.ok().build();
    }

}

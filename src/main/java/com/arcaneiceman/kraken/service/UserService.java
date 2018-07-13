package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.AccountIO;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.repository.UserRepository;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.security.SecurityUtils;
import com.arcaneiceman.kraken.util.RandomUtil;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.Objects;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

@Service
@Transactional
public class UserService {
    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Autowired
    public UserService(UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder,
                       EntityManager entityManager) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    public void requestPasswordReset(AccountIO.ResetPasswordInit.Request requestDTO) {
        User user = userRepository.findUserByEmail(requestDTO.getMail());
        if (user == null)
            throw new SystemException(2, "Could not find user with mail " + requestDTO.getMail(), BAD_REQUEST);
        user.setResetDate(Instant.now());
        user.setResetKey(RandomUtil.generateResetKey());
        user = userRepository.save(user);

        // TODO Send Reset Email
        //emailService.sendPasswordResetEmail(user);
    }

    public void completePasswordReset(AccountIO.ResetPasswordComplete.Request requestDTO) {
        User user = userRepository.findUserByResetKey(requestDTO.getKey());
        if (user == null)
            throw new SystemException(3, "Could not find user with reset key :" + requestDTO.getKey(), BAD_REQUEST);
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        user.setResetKey(null);
        user.setResetDate(null);
        userRepository.save(user);
    }


    public AccountIO.Register.Response createNewUser(AccountIO.Register.Request requestDTO, boolean isAdmin) {
        if (userRepository.findUserByEmail(requestDTO.getLogin()) != null)
            throw new SystemException(35435, "Login " + requestDTO.getLogin() + " already in use", BAD_REQUEST);
        User newUser = new User();
        if (isAdmin)
            newUser.setAuthority(AuthoritiesConstants.ADMIN);
        else
            newUser.setAuthority(AuthoritiesConstants.CONSUMER);
        String encryptedPassword = passwordEncoder.encode(requestDTO.getPassword());
        newUser.setLogin(requestDTO.getLogin());
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(requestDTO.getFirstName());
        newUser.setLastName(requestDTO.getLastName());
        userRepository.save(newUser);

        // Send email
        //emailService.sendWelcomeEmail(newUser);
        return new AccountIO.Register.Response(newUser);
    }

    public void changePassword(AccountIO.ChangePassword.Request requestDTO) {
        User user = getUserOrThrow();

        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user.getLogin(), requestDTO.getOldPassword());
            authenticationManager.authenticate(authenticationToken);
        } catch (Exception ignored) {
            throw new SystemException(2123, "Old password is not correct", BAD_REQUEST);
        }

        if (Objects.equals(requestDTO.getNewPassword(), requestDTO.getOldPassword()))
            throw new SystemException(1231, "Old password and new password cannot be the same", BAD_REQUEST);

        String encryptedPassword = passwordEncoder.encode(requestDTO.getNewPassword());

        user.setPassword(encryptedPassword);
        userRepository.save(user);

        // Remove From Cache
        Session session = entityManager.unwrap(Session.class);
        session.getSessionFactory().getCache().evictEntity(User.class, user.getId());
    }

    @Transactional(readOnly = true)
    public User getUserOrThrow() {
        String currentLogin = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findUserByEmail(currentLogin);
        if (user == null)
            throw new SystemException(6, "Could not recover current user from repository", NOT_FOUND);
        else
            return user;
    }

    @Transactional(readOnly = true)
    public User getUserOrThrow(String name) {
        User user = userRepository.findUserByEmail(name);
        if (user == null)
            throw new SystemException(6, "Could not recover user from repository", NOT_FOUND);
        else
            return user;
    }

}

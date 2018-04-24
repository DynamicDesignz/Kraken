package com.arcaneiceman.kraken.service;

import com.arcaneiceman.kraken.controller.io.AccountIO;
import com.arcaneiceman.kraken.domain.Authority;
import com.arcaneiceman.kraken.domain.User;
import com.arcaneiceman.kraken.repository.AuthorityRepository;
import com.arcaneiceman.kraken.repository.UserRepository;
import com.arcaneiceman.kraken.security.AuthoritiesConstants;
import com.arcaneiceman.kraken.security.SecurityUtils;
import com.arcaneiceman.kraken.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class UserService {
    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthorityRepository authorityRepository;


    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthorityRepository authorityRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
    }

    public void requestPasswordReset(AccountIO.ResetPasswordInit.Request requestDTO) {
        User user = userRepository.findUserByEmail(requestDTO.getMail())
                .orElseThrow(() -> new RuntimeException("Could not find user with mail " + requestDTO.getMail()));
        user.setResetDate(Instant.now());
        user.setResetKey(RandomUtil.generateResetKey());
        user = userRepository.save(user);

        // TODO Send Reset Email
        //emailService.sendPasswordResetEmail(user);
    }

    public void completePasswordReset(AccountIO.ResetPasswordComplete.Request requestDTO) {
        User user = userRepository.findUserByResetKey(requestDTO.getKey())
                .orElseThrow(() -> new RuntimeException("Could not find user with reset key " + requestDTO.getKey()));
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        user.setResetKey(null);
        user.setResetDate(null);
    }


    public AccountIO.Register.Response createNewUser(AccountIO.Register.Request requestDTO) {
        User newUser = new User();
        Authority authority = authorityRepository.findById(AuthoritiesConstants.CONSUMER)
                .orElseThrow(() -> new RuntimeException("Could not find " + AuthoritiesConstants.CONSUMER));
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(requestDTO.getPassword());
        // new user gets initially a generated password
        newUser.setLogin(requestDTO.getLogin());
        newUser.setPassword(encryptedPassword);
        newUser.setActive(false);
        authorities.add(authority);
        newUser.setAuthorities(authorities);
        userRepository.save(newUser);
        return new AccountIO.Register.Response(newUser);
    }

//    /**
//     * Update all information for a specific user, and return the modified user.
//     *
//     * @param userDTO user to update
//     * @return updated user
//     */
//    public Optional<UserIO.Update.Response> updateUser(UserIO.Update.Request userDTO) {
//        return userRepository.findOne(userDTO.getId()).map(user ->
//        ).map()
//
//
//
//        return Optional.of(userRepository.findOne(userDTO.getId()).get())
//                .map(user -> {
//                    user.setLogin(userDTO.getLogin());
//                    user.setEmail(userDTO.getEmail());
//                    Set<Authority> managedAuthorities = user.getAuthorities();
//                    managedAuthorities.clear();
//                    userDTO.getAuthorities().stream()
//                            .map(authorityRepository::findOne)
//                            .forEach(managedAuthorities::add);
//                    //cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).evict(user.getLogin());
//                    //cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).evict(user.getEmail());
//                    log.debug("Changed Information for User: {}", user);
//                    return user;
//                })
//                .map(UserIO.Update.Response::new);
//    }

    public void changePassword(AccountIO.ChangePassword.Request requestDTO) {
        SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findUserByLogin)
                .ifPresent(user -> {
                    String encryptedPassword = passwordEncoder.encode(requestDTO.getNewPassword());
                    user.setPassword(encryptedPassword);
                    //cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).evict(user.getLogin());
                    //cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).evict(user.getEmail());
                    log.debug("Changed password for User: {}", user);
                });
    }
//
//    @Transactional(readOnly = true)
//    public Page<UserIO.Get.Response> getAllUsers(String) {
//        return userRepository.findUserByAuthoritiesContaining(pageable, Constants.ANONYMOUS_USER).map(UserDTO::new);
//    }

    //    @Transactional(readOnly = true)
//    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
//        return userRepository.findUserWithAuthoritiesByLogin(login);
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<User> getUserByEmail(String email) {
//        return userRepository.findUserByEmailIgnoreCase(email);
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<User> getUserWithAuthorities(Long id) {
//        return userRepository.findUserWithAuthoritiesById(id);
//    }
//
    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findUserWithAuthoritiesByLogin);
    }

//    /**
//     * @return a list of all the authorities
//     */
//    public List<String> getAuthorities() {
//        return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
//    }
}

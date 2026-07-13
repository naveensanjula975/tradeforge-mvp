package com.tradeforge.auth.service;

import com.tradeforge.auth.domain.User;
import com.tradeforge.auth.domain.UserRepository;
import com.tradeforge.auth.web.dto.AuthResponse;
import com.tradeforge.auth.web.dto.LoginRequest;
import com.tradeforge.auth.web.dto.RegisterRequest;
import com.tradeforge.common.exception.BusinessRuleException;
import com.tradeforge.common.exception.ConflictException;
import com.tradeforge.common.exception.ErrorCode;
import com.tradeforge.account.domain.Account;
import com.tradeforge.account.domain.AccountRepository;
import com.tradeforge.auth.domain.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Handles user registration and login.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Initial demo cash balance for new accounts (in local / seed mode). */
    public static final BigDecimal INITIAL_DEMO_BALANCE = new BigDecimal("100000.0000");

    private final UserRepository    userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;

    public UserService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository    = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder   = passwordEncoder;
        this.jwtService        = jwtService;
    }

    /**
     * Register a new trader.
     *
     * <ol>
     *   <li>Reject duplicate email with HTTP 409.</li>
     *   <li>Hash the password with BCrypt.</li>
     *   <li>Persist the user.</li>
     *   <li>Create a trading account with the initial demo balance.</li>
     *   <li>Return a JWT token.</li>
     * </ol>
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalisedEmail = User.normaliseEmail(request.email());

        if (userRepository.existsByEmail(normalisedEmail)) {
            throw new ConflictException(
                    ErrorCode.EMAIL_ALREADY_EXISTS,
                    "An account with email '" + normalisedEmail + "' already exists.");
        }

        User user = User.create(
                request.name(),
                normalisedEmail,
                passwordEncoder.encode(request.password()),
                UserRole.TRADER);

        userRepository.save(user);

        // Create the associated trading account
        Account account = Account.create(user.getId(), INITIAL_DEMO_BALANCE);
        accountRepository.save(account);

        log.info("Registered new user [{}] with account [{}]", user.getId(), account.getId());

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, jwtService.getExpirationMs(), user.getId(), user.getEmail(), user.getRole().name());
    }

    /**
     * Authenticate a user and return a JWT.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalisedEmail = User.normaliseEmail(request.email());

        User user = userRepository.findByEmail(normalisedEmail)
                .orElseThrow(() -> new BusinessRuleException(
                        ErrorCode.AUTHENTICATION_FAILED, "Invalid credentials."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessRuleException(ErrorCode.AUTHENTICATION_FAILED, "Invalid credentials.");
        }

        log.info("User [{}] logged in", user.getId());

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, jwtService.getExpirationMs(), user.getId(), user.getEmail(), user.getRole().name());
    }
}

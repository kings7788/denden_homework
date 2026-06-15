package com.bryant.denden_homework.service;

import com.bryant.denden_homework.entity.TokenType;
import com.bryant.denden_homework.entity.User;
import com.bryant.denden_homework.entity.UserStatus;
import com.bryant.denden_homework.entity.VerificationToken;
import com.bryant.denden_homework.repository.UserRepository;
import com.bryant.denden_homework.repository.VerificationTokenRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and account-activation logic.
 * Login / 2FA arrives in milestone 3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int ACTIVATION_TTL_HOURS = 24;
    private static final int OTP_TTL_MINUTES = 5;

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Register a new member (PENDING) and send an activation email. */
    @Transactional
    public User register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already registered");
        }
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .status(UserStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        String token = UUID.randomUUID().toString();
        tokenRepository.save(VerificationToken.builder()
                .token(token)
                .user(user)
                .type(TokenType.ACTIVATION)
                .expiresAt(LocalDateTime.now().plusHours(ACTIVATION_TTL_HOURS))
                .used(false)
                .build());

        emailService.sendActivationEmail(email, token);
        log.info("Registered new member: {} (status=PENDING)", email);
        return user;
    }

    /** Activate an account by consuming its activation token. */
    @Transactional
    public User verify(String token) {
        VerificationToken vt = tokenRepository.findByTokenAndType(token, TokenType.ACTIVATION)
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation token"));
        if (vt.isUsed()) {
            throw new IllegalStateException("Activation token already used");
        }
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Activation token expired");
        }
        User user = vt.getUser();
        user.setStatus(UserStatus.ACTIVE);
        vt.setUsed(true);
        log.info("Activated member: {}", user.getEmail());
        return user;
    }

    /**
     * Login first factor: verify email + password on an ACTIVE account, then email
     * a one-time code. Returns a challengeId used in the second-factor step.
     */
    @Transactional
    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Account not activated. Please verify your email first.");
        }

        String challengeId = UUID.randomUUID().toString();
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        tokenRepository.save(VerificationToken.builder()
                .token(challengeId)
                .code(otp)
                .user(user)
                .type(TokenType.LOGIN_OTP)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .used(false)
                .build());

        emailService.sendLoginOtp(email, otp);
        log.info("Login OTP issued for {} (challengeId={})", email, challengeId);
        return challengeId;
    }

    /**
     * Login second factor: validate the OTP for the given challenge, record the
     * login time, and return the user (caller mints the JWT).
     */
    @Transactional
    public User verifyLogin(String challengeId, String otp) {
        VerificationToken vt = tokenRepository.findByTokenAndType(challengeId, TokenType.LOGIN_OTP)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired login challenge"));
        if (vt.isUsed()) {
            throw new IllegalStateException("Verification code already used");
        }
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification code expired");
        }
        if (!vt.getCode().equals(otp)) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        vt.setUsed(true);
        User user = vt.getUser();
        user.setLastLoginAt(LocalDateTime.now());
        log.info("Login verified for {}", user.getEmail());
        return user;
    }
}

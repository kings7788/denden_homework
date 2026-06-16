package com.bryant.denden_homework.service;

import com.bryant.denden_homework.entity.TokenType;
import com.bryant.denden_homework.entity.User;
import com.bryant.denden_homework.entity.UserStatus;
import com.bryant.denden_homework.entity.VerificationToken;
import com.bryant.denden_homework.repository.UserRepository;
import com.bryant.denden_homework.repository.VerificationTokenRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, account activation, and two-step login logic. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int ACTIVATION_TTL_HOURS = 24;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailContentFactory emailContentFactory;
    private final OtpGenerator otpGenerator;

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

        // Activation token is a random UUID (the lookup key), so it is stored as-is.
        String token = UUID.randomUUID().toString();
        tokenRepository.save(VerificationToken.builder()
                .token(token)
                .user(user)
                .type(TokenType.ACTIVATION)
                .expiresAt(LocalDateTime.now().plusHours(ACTIVATION_TTL_HOURS))
                .used(false)
                .build());

        emailService.send(email, emailContentFactory.activation(token));
        log.info("Registered new member: {} (status=PENDING)", email);
        return user;
    }

    /** Activate an account by consuming its activation token. */
    @Transactional
    public User verify(String token) {
        VerificationToken vt = tokenRepository.findByTokenAndType(token, TokenType.ACTIVATION)
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation token"));
        assertUsable(vt);
        User user = vt.getUser();
        user.setStatus(UserStatus.ACTIVE);
        vt.setUsed(true);
        log.info("Activated member: {}", user.getEmail());
        return user;
    }

    /**
     * Login first factor: verify email + password on an ACTIVE account, invalidate any
     * outstanding OTP, then email a fresh one-time code. Returns the challengeId.
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

        // Only one OTP may be active at a time: burn any earlier un-used ones.
        tokenRepository.findByUser_EmailAndTypeAndUsedFalse(email, TokenType.LOGIN_OTP)
                .forEach(t -> t.setUsed(true));

        String challengeId = UUID.randomUUID().toString();
        String otp = otpGenerator.generate();
        tokenRepository.save(VerificationToken.builder()
                .token(challengeId)
                .code(passwordEncoder.encode(otp)) // store the OTP hashed, never plaintext
                .user(user)
                .type(TokenType.LOGIN_OTP)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .used(false)
                .build());

        emailService.send(email, emailContentFactory.loginOtp(otp, OTP_TTL_MINUTES));
        log.info("Login OTP issued for {} (challengeId={})", email, challengeId);
        return challengeId;
    }

    /**
     * Login second factor: validate the OTP for the given challenge, record the login
     * time, and return the user. A wrong OTP increments an attempt counter and burns the
     * challenge after {@value #MAX_OTP_ATTEMPTS} tries (noRollbackFor keeps the counter).
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public User verifyLogin(String challengeId, String otp) {
        VerificationToken vt = tokenRepository.findByTokenAndType(challengeId, TokenType.LOGIN_OTP)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired login challenge"));
        assertUsable(vt);

        if (!passwordEncoder.matches(otp, vt.getCode())) {
            vt.setAttempts(vt.getAttempts() + 1);
            if (vt.getAttempts() >= MAX_OTP_ATTEMPTS) {
                vt.setUsed(true); // burn the challenge after too many wrong tries
                log.warn("Login challenge {} burned after {} failed attempts", challengeId, vt.getAttempts());
            }
            throw new IllegalArgumentException("Invalid verification code");
        }

        vt.setUsed(true);
        User user = vt.getUser();
        user.setLastLoginAt(LocalDateTime.now());
        log.info("Login verified for {}", user.getEmail());
        return user;
    }

    /** Shared guard: a token must be neither used nor expired. */
    private void assertUsable(VerificationToken vt) {
        if (vt.isUsed()) {
            throw new IllegalStateException("Token already used");
        }
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expired");
        }
    }
}

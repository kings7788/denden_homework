package com.bryant.denden_homework.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Placeholder email sender: logs the activation link / OTP instead of actually
 * sending. Active by default ({@code app.email.provider=log}); other providers
 * (gmail, mailjet) take over when selected.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailService implements EmailService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public void sendActivationEmail(String toEmail, String activationToken) {
        log.info("[EMAIL:ACTIVATION] to={} link={}/api/auth/verify?token={}",
                toEmail, baseUrl, activationToken);
    }

    @Override
    public void sendLoginOtp(String toEmail, String otp) {
        log.info("[EMAIL:LOGIN_OTP] to={} otp={}", toEmail, otp);
    }
}

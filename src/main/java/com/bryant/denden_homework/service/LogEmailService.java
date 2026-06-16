package com.bryant.denden_homework.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Placeholder email sender: logs the message instead of actually sending.
 * Active by default ({@code app.email.provider=log}).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailService implements EmailService {

    @Override
    public void send(String toEmail, EmailMessage message) {
        log.info("[EMAIL] to={} subject='{}' | {}", toEmail, message.subject(), message.textBody());
    }
}

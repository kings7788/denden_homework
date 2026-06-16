package com.bryant.denden_homework.service;

/**
 * Transport for sending a pre-built {@link EmailMessage}. Implementations only
 * handle delivery; content is built by {@link EmailContentFactory}.
 * Selectable via {@code app.email.provider} (log | gmail | mailjet).
 */
public interface EmailService {

    void send(String toEmail, EmailMessage message);
}

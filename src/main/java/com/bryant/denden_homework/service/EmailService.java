package com.bryant.denden_homework.service;

/**
 * Abstraction over the email provider. Milestone 1 uses a logging implementation;
 * milestone 2 swaps in a Mailjet-backed implementation without touching callers.
 */
public interface EmailService {

    /** Send the account-activation link containing the given activation token. */
    void sendActivationEmail(String toEmail, String activationToken);

    /** Send the one-time login code (second factor). */
    void sendLoginOtp(String toEmail, String otp);
}

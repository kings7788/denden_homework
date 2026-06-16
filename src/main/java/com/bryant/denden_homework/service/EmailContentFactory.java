package com.bryant.denden_homework.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for email subjects/bodies and the activation link.
 * Keeps content building out of the transport implementations (Gmail/Mailjet/log),
 * so the link format and copy are defined exactly once.
 */
@Component
public class EmailContentFactory {

    private final String baseUrl;

    public EmailContentFactory(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public EmailMessage activation(String activationToken) {
        String link = baseUrl + "/api/auth/verify?token=" + activationToken;
        return new EmailMessage(
                "Activate your account",
                "Welcome! Please activate your account by visiting: " + link,
                "<p>Welcome!</p><p>Please activate your account:</p>"
                        + "<p><a href=\"" + link + "\">Activate my account</a></p>"
                        + "<p>Or open this link: " + link + "</p>");
    }

    public EmailMessage loginOtp(String otp, int ttlMinutes) {
        return new EmailMessage(
                "Your login verification code",
                "Your login verification code is: " + otp + " (valid for " + ttlMinutes + " minutes).",
                "<p>Your login verification code is:</p><h2>" + otp + "</h2>"
                        + "<p>It is valid for " + ttlMinutes + " minutes.</p>");
    }
}

package com.bryant.denden_homework.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Real email sender backed by the Mailjet Send API v3.1.
 * Active only when {@code app.email.provider=mailjet} (so tests / local runs
 * without credentials fall back to {@link LogEmailService}).
 */
@Slf4j
@Service("mailjetEmailService")
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mailjet")
public class MailjetEmailService implements EmailService {

    private static final String SEND_URL = "https://api.mailjet.com/v3.1/send";

    private final RestClient restClient;
    private final String senderEmail;
    private final String senderName;
    private final String baseUrl;

    public MailjetEmailService(
            @Value("${mailjet.api-key}") String apiKey,
            @Value("${mailjet.secret-key}") String secretKey,
            @Value("${mailjet.sender-email}") String senderEmail,
            @Value("${mailjet.sender-name:Denden Homework}") String senderName,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        String basicAuth = Base64.getEncoder()
                .encodeToString((apiKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(SEND_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .build();
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendActivationEmail(String toEmail, String activationToken) {
        String link = baseUrl + "/api/auth/verify?token=" + activationToken;
        send(toEmail, "Activate your account",
                "Welcome! Please activate your account by visiting: " + link,
                "<p>Welcome!</p><p>Please activate your account:</p>"
                        + "<p><a href=\"" + link + "\">Activate my account</a></p>");
    }

    @Override
    public void sendLoginOtp(String toEmail, String otp) {
        send(toEmail, "Your login verification code",
                "Your login verification code is: " + otp + " (valid for 5 minutes).",
                "<p>Your login verification code is:</p><h2>" + otp + "</h2>"
                        + "<p>It is valid for 5 minutes.</p>");
    }

    private void send(String toEmail, String subject, String textPart, String htmlPart) {
        Map<String, Object> message = Map.of(
                "From", Map.of("Email", senderEmail, "Name", senderName),
                "To", List.of(Map.of("Email", toEmail)),
                "Subject", subject,
                "TextPart", textPart,
                "HTMLPart", htmlPart);
        Map<String, Object> payload = Map.of("Messages", List.of(message));

        restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        log.info("Sent Mailjet email to={} subject='{}'", toEmail, subject);
    }
}

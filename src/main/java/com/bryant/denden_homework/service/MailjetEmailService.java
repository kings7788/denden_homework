package com.bryant.denden_homework.service;

import com.bryant.denden_homework.exception.EmailDeliveryException;
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
 * Sends email via the Mailjet Send API v3.1.
 * Active only when {@code app.email.provider=mailjet}.
 */
@Slf4j
@Service("mailjetEmailService")
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mailjet")
public class MailjetEmailService implements EmailService {

    private static final String SEND_URL = "https://api.mailjet.com/v3.1/send";

    private final RestClient restClient;
    private final String senderEmail;
    private final String senderName;

    public MailjetEmailService(
            @Value("${mailjet.api-key}") String apiKey,
            @Value("${mailjet.secret-key}") String secretKey,
            @Value("${mailjet.sender-email}") String senderEmail,
            @Value("${mailjet.sender-name:Denden Homework}") String senderName) {
        String basicAuth = Base64.getEncoder()
                .encodeToString((apiKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(SEND_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .build();
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    @Override
    public void send(String toEmail, EmailMessage message) {
        Map<String, Object> mailjetMessage = Map.of(
                "From", Map.of("Email", senderEmail, "Name", senderName),
                "To", List.of(Map.of("Email", toEmail)),
                "Subject", message.subject(),
                "TextPart", message.textBody(),
                "HTMLPart", message.htmlBody());
        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("Messages", List.of(mailjetMessage)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent Mailjet email to={} subject='{}'", toEmail, message.subject());
        } catch (Exception e) {
            log.error("Mailjet send failed to={} : {}", toEmail, e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send email: " + e.getMessage(), e);
        }
    }
}

package com.bryant.denden_homework.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email sender backed by Gmail's SMTP server (smtp.gmail.com:587) via JavaMailSender.
 * Active when {@code app.email.provider=gmail}. Authenticates with a Gmail
 * App Password (configured in the git-ignored application-dev.properties).
 */
@Slf4j
@Service("gmailSmtpEmailService")
@ConditionalOnProperty(name = "app.email.provider", havingValue = "gmail")
public class GmailSmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String baseUrl;

    public GmailSmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.email.from:${spring.mail.username}}") String fromEmail,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendActivationEmail(String toEmail, String activationToken) {
        String link = baseUrl + "/api/auth/verify?token=" + activationToken;
        send(toEmail, "Activate your account",
                "<p>Welcome!</p><p>Please activate your account:</p>"
                        + "<p><a href=\"" + link + "\">Activate my account</a></p>"
                        + "<p>Or open this link: " + link + "</p>");
    }

    @Override
    public void sendLoginOtp(String toEmail, String otp) {
        send(toEmail, "Your login verification code",
                "<p>Your login verification code is:</p><h2>" + otp + "</h2>"
                        + "<p>It is valid for 5 minutes.</p>");
    }

    private void send(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Sent Gmail SMTP email to={} subject='{}'", toEmail, subject);
        } catch (Exception e) {
            log.error("Gmail SMTP send failed to={} : {}", toEmail, e.getMessage(), e);
            throw new IllegalStateException("Failed to send email: " + e.getMessage(), e);
        }
    }
}

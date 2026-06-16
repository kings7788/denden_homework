package com.bryant.denden_homework.service;

import com.bryant.denden_homework.exception.EmailDeliveryException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends email via Gmail's SMTP server (smtp.gmail.com:587) using JavaMailSender.
 * Active when {@code app.email.provider=gmail}.
 */
@Slf4j
@Service("gmailSmtpEmailService")
@ConditionalOnProperty(name = "app.email.provider", havingValue = "gmail")
public class GmailSmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public GmailSmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.email.from:${spring.mail.username}}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String toEmail, EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            mailSender.send(mime);
            log.info("Sent Gmail SMTP email to={} subject='{}'", toEmail, message.subject());
        } catch (Exception e) {
            log.error("Gmail SMTP send failed to={} : {}", toEmail, e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send email: " + e.getMessage(), e);
        }
    }
}

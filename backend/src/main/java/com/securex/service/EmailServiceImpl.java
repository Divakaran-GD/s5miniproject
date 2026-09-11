package com.securex.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${securex.mail.from:noreply@securex.com}")
    private String mailFrom;

    @Value("${spring.mail.host:}")
    private String mailHost;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendShareLinkEmail(String recipientEmail, String fileName, String shareUrl, LocalDateTime expiresAt, Integer maxDownloads) {
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("Email service is unconfigured (MAIL_HOST is missing). Unable to send email to {}", recipientEmail);
            throw new IllegalStateException("Email service host is not configured");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom.trim());
            }
            message.setTo(recipientEmail.trim());
            message.setSubject("Secure File Shared: " + (fileName != null ? fileName : "Shared File"));

            StringBuilder body = new StringBuilder();
            body.append("Your secure file has been shared with you.\n\n");
            body.append("Access the file using this temporary link:\n\n");
            body.append(shareUrl).append("\n\n");
            body.append("This link will expire according to the configured expiration time");
            if (expiresAt != null) {
                body.append(" (")
                    .append(expiresAt.format(DATE_FORMATTER))
                    .append(" UTC)");
            }
            body.append(".\n");
            if (maxDownloads != null) {
                body.append("Maximum download limit: ").append(maxDownloads).append("\n");
            }
            body.append("\nThank you for using SecureX Portal.");

            message.setText(body.toString());

            mailSender.send(message);
            log.info("Successfully sent share link email to recipient: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send share link email to recipient {}: {}", recipientEmail, e.getMessage(), e);
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}

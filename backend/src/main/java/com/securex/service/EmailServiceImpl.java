package com.securex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final RestClient restClient;

    @Value("${securex.mail.resend-api-key:}")
    private String resendApiKey;

    @Value("${securex.mail.from:onboarding@resend.dev}")
    private String mailFrom;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EmailServiceImpl() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .build();
    }

    public EmailServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void sendShareLinkEmail(String recipientEmail, String fileName, String shareUrl, LocalDateTime expiresAt, Integer maxDownloads) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Email service is unconfigured (RESEND_API_KEY is missing). Unable to send email to {}", recipientEmail);
            throw new IllegalStateException("Email service API key is not configured");
        }

        try {
            String subject = "Secure File Shared: " + (fileName != null ? fileName : "Shared File");

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

            String fromAddress = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom.trim() : "onboarding@resend.dev";

            Map<String, Object> payload = Map.of(
                    "from", fromAddress,
                    "to", List.of(recipientEmail.trim()),
                    "subject", subject,
                    "text", body.toString()
            );

            restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully sent share link email via Resend API to recipient: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send share link email to recipient {}: {}", recipientEmail, e.getMessage(), e);
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}

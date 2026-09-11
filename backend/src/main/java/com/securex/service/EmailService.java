package com.securex.service;

import java.time.LocalDateTime;

public interface EmailService {
    void sendShareLinkEmail(String recipientEmail, String fileName, String shareUrl, LocalDateTime expiresAt, Integer maxDownloads);
}

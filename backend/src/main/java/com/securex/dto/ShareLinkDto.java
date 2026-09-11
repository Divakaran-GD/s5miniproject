package com.securex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLinkDto {
    private String uuid;
    private String fileUuid;
    private String originalFilename;
    private Long fileSize;
    private String rawToken; // Only populated upon creation response
    private String shareUrl; // Convenient full share URL
    private LocalDateTime expiresAt;
    private Integer maxDownloads;
    private Integer downloadCount;
    private boolean passwordProtected;
    private String recipientEmail;
    private boolean revoked;
    private boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
}

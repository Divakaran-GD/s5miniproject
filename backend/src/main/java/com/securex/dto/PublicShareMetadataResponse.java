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
public class PublicShareMetadataResponse {
    private String shareUuid;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private String encryptionAlgorithm;
    private String encryptionVersion;
    private String iv;
    private String sha256Hash;
    private LocalDateTime expiresAt;
    private Integer remainingDownloads;
    private boolean passwordProtected;
    private boolean recipientRestricted;
    private boolean valid;
    private String statusMessage;
}

package com.securex.service;

import com.securex.dto.CreateShareRequest;
import com.securex.dto.PublicShareMetadataResponse;
import com.securex.dto.ShareLinkDto;
import com.securex.dto.VerifyPasswordRequest;
import com.securex.entity.DownloadEvent;
import com.securex.entity.FileEntity;
import com.securex.entity.ShareLink;
import com.securex.entity.User;
import com.securex.exception.*;
import com.securex.repository.DownloadEventRepository;
import com.securex.repository.FileRepository;
import com.securex.repository.ShareLinkRepository;
import com.securex.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final DownloadEventRepository downloadEventRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Value("${securex.public-base-url:}")
    private String publicBaseUrlConfig;

    @Value("${securex.frontend-url:http://localhost:5173}")
    private String frontendUrlConfig;

    @Override
    @Transactional
    public ShareLinkDto createShare(String fileUuid, CreateShareRequest request, HttpServletRequest httpRequest) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedAccessException("Authentication required to create share links");
        }

        FileEntity file = fileRepository.findByUuidAndDeletedAtIsNull(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with UUID: " + fileUuid));

        if (!file.getOwner().getId().equals(currentUser.getId()) && !currentUser.getRole().name().equals("ADMIN")) {
            throw new UnauthorizedAccessException("You can only share your own files");
        }

        // Generate 256-bit CSPRNG high-entropy share token
        String rawToken = SecurityUtils.generateRandomToken(32);
        String tokenHash = SecurityUtils.hashSha256(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(request.getExpiresInMinutes());

        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        ShareLink shareLink = ShareLink.builder()
                .file(file)
                .owner(currentUser)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .maxDownloads(request.getMaxDownloads())
                .downloadCount(0)
                .passwordHash(passwordHash)
                .recipientEmail(request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank() ? request.getRecipientEmail().trim().toLowerCase() : null)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        shareLink = shareLinkRepository.save(shareLink);

        auditLogService.logEvent(currentUser.getId(), "SHARE_CREATED", "SHARE_LINK", shareLink.getUuid(),
                "Created share link for file: " + file.getOriginalFilename() + " (Expires: " + expiresAt + ", Max Downloads: " + request.getMaxDownloads() + ")", httpRequest);

        ShareLinkDto dto = mapToDto(shareLink);
        dto.setRawToken(rawToken);
        String baseUrl = getEffectiveBaseUrl(httpRequest);
        dto.setShareUrl(baseUrl + "/share/" + rawToken);

        if (shareLink.getRecipientEmail() != null && !shareLink.getRecipientEmail().isBlank()) {
            try {
                emailService.sendShareLinkEmail(
                        shareLink.getRecipientEmail(),
                        file.getOriginalFilename(),
                        dto.getShareUrl(),
                        expiresAt,
                        request.getMaxDownloads()
                );
                dto.setEmailSent(true);
                dto.setEmailStatusMessage("Share link generated and sent successfully to " + shareLink.getRecipientEmail() + ".");
            } catch (Exception e) {
                log.warn("Share link created, but failed to send email to {}: {}", shareLink.getRecipientEmail(), e.getMessage());
                dto.setEmailSent(false);
                dto.setEmailStatusMessage("Share link generated successfully, but the email could not be sent.");
            }
        }

        return dto;
    }

    @Override
    public List<ShareLinkDto> getSharesForFile(String fileUuid) {
        User currentUser = SecurityUtils.getCurrentUser();
        FileEntity file = fileRepository.findByUuidAndDeletedAtIsNull(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (currentUser == null || (!file.getOwner().getId().equals(currentUser.getId()) && !currentUser.getRole().name().equals("ADMIN"))) {
            throw new UnauthorizedAccessException("You are not authorized to view shares for this file");
        }

        return shareLinkRepository.findByFileOrderByCreatedAtDesc(file).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShareLinkDto> getMyShares() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedAccessException("Authentication required");
        }
        return shareLinkRepository.findByOwnerOrderByCreatedAtDesc(currentUser).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeShare(String shareUuid, HttpServletRequest httpRequest) {
        User currentUser = SecurityUtils.getCurrentUser();
        ShareLink shareLink = shareLinkRepository.findByUuid(shareUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (currentUser == null || (!shareLink.getOwner().getId().equals(currentUser.getId()) && !currentUser.getRole().name().equals("ADMIN"))) {
            throw new UnauthorizedAccessException("You are not authorized to revoke this share link");
        }

        shareLink.setRevoked(true);
        shareLinkRepository.save(shareLink);

        auditLogService.logEvent(currentUser.getId(), "SHARE_REVOKED", "SHARE_LINK", shareLink.getUuid(),
                "Revoked share link for file: " + shareLink.getFile().getOriginalFilename(), httpRequest);
    }

    @Override
    public PublicShareMetadataResponse getPublicShareMetadata(String token) {
        String tokenHash = SecurityUtils.hashSha256(token);
        ShareLink shareLink = shareLinkRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent share link token"));

        FileEntity file = shareLink.getFile();
        if (file.getDeletedAt() != null) {
            throw new ResourceNotFoundException("The requested shared file has been deleted");
        }

        boolean valid = true;
        String statusMessage = "Share link is active and valid";

        if (shareLink.isRevoked()) {
            valid = false;
            statusMessage = "This share link has been revoked by the owner";
        } else if (shareLink.isExpired()) {
            valid = false;
            statusMessage = "This share link has expired";
        } else if (shareLink.isMaxDownloadsReached()) {
            valid = false;
            statusMessage = "Maximum download limit for this link has been reached";
        }

        int remainingDownloads = Math.max(0, shareLink.getMaxDownloads() - shareLink.getDownloadCount());

        return PublicShareMetadataResponse.builder()
                .shareUuid(shareLink.getUuid())
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .encryptionAlgorithm(file.getEncryptionAlgorithm())
                .encryptionVersion(file.getEncryptionVersion())
                .iv(file.getIv())
                .sha256Hash(file.getSha256Hash())
                .expiresAt(shareLink.getExpiresAt())
                .remainingDownloads(remainingDownloads)
                .passwordProtected(shareLink.isPasswordProtected())
                .recipientRestricted(shareLink.getRecipientEmail() != null)
                .valid(valid)
                .statusMessage(statusMessage)
                .build();
    }

    @Override
    public boolean verifySharePassword(String token, VerifyPasswordRequest request, HttpServletRequest httpRequest) {
        String tokenHash = SecurityUtils.hashSha256(token);
        ShareLink shareLink = shareLinkRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (!shareLink.isPasswordProtected()) {
            return true;
        }

        boolean matches = passwordEncoder.matches(request.getPassword(), shareLink.getPasswordHash());

        if (!matches) {
            auditLogService.logEvent(null, "PASSWORD_VERIFICATION_FAILED", "SHARE_LINK", shareLink.getUuid(),
                    "Incorrect password entered for share link", httpRequest);
            throw new InvalidPasswordException("Invalid share password");
        }

        return true;
    }

    @Override
    @Transactional
    public Resource downloadPublicShareFile(String token, String password, HttpServletRequest httpRequest) {
        String tokenHash = SecurityUtils.hashSha256(token);
        ShareLink shareLink = shareLinkRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or non-existent share link token"));

        if (shareLink.isRevoked()) {
            logDownloadEvent(shareLink, false, "SHARE_REVOKED", httpRequest);
            throw new ShareLinkRevokedException("This share link has been revoked");
        }

        if (shareLink.isExpired()) {
            logDownloadEvent(shareLink, false, "SHARE_EXPIRED", httpRequest);
            throw new ShareLinkExpiredException("This share link has expired");
        }

        if (shareLink.isMaxDownloadsReached()) {
            logDownloadEvent(shareLink, false, "DOWNLOAD_LIMIT_REACHED", httpRequest);
            throw new ShareLinkMaxDownloadsReachedException("Maximum download count reached for this share link");
        }

        if (shareLink.isPasswordProtected()) {
            if (password == null || password.isBlank() || !passwordEncoder.matches(password, shareLink.getPasswordHash())) {
                logDownloadEvent(shareLink, false, "INVALID_PASSWORD", httpRequest);
                throw new InvalidPasswordException("Valid password required to download file");
            }
        }

        // Perform safe atomic update to increment download count
        int updated = shareLinkRepository.incrementDownloadCountSafely(shareLink.getId(), LocalDateTime.now());
        if (updated == 0) {
            logDownloadEvent(shareLink, false, "CONCURRENT_LIMIT_EXCEEDED", httpRequest);
            throw new ShareLinkMaxDownloadsReachedException("Maximum download limit reached");
        }

        FileEntity file = shareLink.getFile();
        Resource resource = fileStorageService.loadFileAsResource(file);

        logDownloadEvent(shareLink, true, null, httpRequest);

        auditLogService.logEvent(shareLink.getOwner().getId(), "DOWNLOAD_SUCCESS", "SHARE_LINK", shareLink.getUuid(),
                "Successfully downloaded file via share link: " + file.getOriginalFilename(), httpRequest);

        return resource;
    }

    private void logDownloadEvent(ShareLink shareLink, boolean success, String failureReason, HttpServletRequest httpRequest) {
        DownloadEvent event = DownloadEvent.builder()
                .shareLink(shareLink)
                .file(shareLink.getFile())
                .accessedAt(LocalDateTime.now())
                .success(success)
                .failureReason(failureReason)
                .ipHash(httpRequest != null ? SecurityUtils.getClientIpHash(httpRequest) : null)
                .userAgentHash(httpRequest != null ? SecurityUtils.getUserAgentHash(httpRequest) : null)
                .build();
        downloadEventRepository.save(event);
    }

    private ShareLinkDto mapToDto(ShareLink share) {
        boolean isExpired = share.isExpired();
        return ShareLinkDto.builder()
                .uuid(share.getUuid())
                .fileUuid(share.getFile().getUuid())
                .originalFilename(share.getFile().getOriginalFilename())
                .fileSize(share.getFile().getFileSize())
                .expiresAt(share.getExpiresAt())
                .maxDownloads(share.getMaxDownloads())
                .downloadCount(share.getDownloadCount())
                .passwordProtected(share.isPasswordProtected())
                .recipientEmail(share.getRecipientEmail())
                .revoked(share.isRevoked())
                .expired(isExpired)
                .createdAt(share.getCreatedAt())
                .lastAccessedAt(share.getLastAccessedAt())
                .build();
    }

    private String getEffectiveBaseUrl(HttpServletRequest httpRequest) {
        if (publicBaseUrlConfig != null && !publicBaseUrlConfig.isBlank()) {
            String trimmed = publicBaseUrlConfig.trim();
            if (!isLocalhostUrl(trimmed) && !isPrivateIpUrl(trimmed)) {
                return stripTrailingSlash(trimmed);
            }
        }

        if (httpRequest != null) {
            String origin = httpRequest.getHeader("Origin");
            if (origin == null || origin.isBlank()) {
                String referer = httpRequest.getHeader("Referer");
                if (referer != null && !referer.isBlank()) {
                    try {
                        java.net.URI uri = new java.net.URI(referer);
                        origin = uri.getScheme() + "://" + uri.getAuthority();
                    } catch (Exception ignored) {}
                }
            }

            if (origin != null && !origin.isBlank() && !isLocalhostUrl(origin) && !isPrivateIpUrl(origin)) {
                return stripTrailingSlash(origin);
            }

            String forwardedHost = httpRequest.getHeader("X-Forwarded-Host");
            String host = (forwardedHost != null && !forwardedHost.isBlank())
                    ? forwardedHost.split(",")[0].trim()
                    : httpRequest.getHeader("Host");

            if (host != null && !host.isBlank() && !isLocalhostHeader(host) && !isPrivateIpHeader(host)) {
                String proto = httpRequest.getHeader("X-Forwarded-Proto");
                String scheme = (proto != null && !proto.isBlank()) ? proto : httpRequest.getScheme();
                if (scheme == null || scheme.isBlank()) scheme = "https";
                return scheme + "://" + host;
            }
        }

        if (publicBaseUrlConfig != null && !publicBaseUrlConfig.isBlank()) {
            return stripTrailingSlash(publicBaseUrlConfig.trim());
        }

        if (frontendUrlConfig != null && !frontendUrlConfig.isBlank()) {
            return stripTrailingSlash(frontendUrlConfig.trim());
        }

        throw new IllegalStateException(
                "PUBLIC_BASE_URL environment variable is missing or unconfigured for public share link generation. " +
                "Please set PUBLIC_BASE_URL=https://<your-deployed-domain> in your production environment variables (e.g., https://my-app.onrender.com)."
        );
    }

    private boolean isPrivateIpUrl(String url) {
        if (url == null) return false;
        return url.matches(".*://(192\\.168\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.).*");
    }

    private boolean isPrivateIpHeader(String hostHeader) {
        if (hostHeader == null) return false;
        return hostHeader.matches("(192\\.168\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.).*");
    }

    private boolean isLocalhostUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("://localhost") || lower.contains("://127.0.0.1") || lower.contains("://0.0.0.0");
    }

    private boolean isLocalhostHeader(String hostHeader) {
        if (hostHeader == null) return false;
        String lower = hostHeader.toLowerCase();
        return lower.startsWith("localhost") || lower.startsWith("127.0.0.1") || lower.startsWith("0.0.0.0");
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String getLocalLanIp() {
        try {
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            if (localHost != null && !localHost.isLoopbackAddress() && localHost instanceof java.net.Inet4Address && localHost.isSiteLocalAddress()) {
                return localHost.getHostAddress();
            }

            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;
                java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}

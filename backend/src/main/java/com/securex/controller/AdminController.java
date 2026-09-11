package com.securex.controller;

import com.securex.dto.AuditLogDto;
import com.securex.dto.FileDto;
import com.securex.dto.ShareLinkDto;
import com.securex.dto.UserDto;
import com.securex.entity.User;
import com.securex.repository.DownloadEventRepository;
import com.securex.repository.FileRepository;
import com.securex.repository.ShareLinkRepository;
import com.securex.repository.UserRepository;
import com.securex.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Operations", description = "Administrator security metrics, user management, and audit log inspection")
public class AdminController {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final DownloadEventRepository downloadEventRepository;
    private final AuditLogService auditLogService;

    @GetMapping("/stats")
    @Operation(summary = "Get high-level administrator security and activity metrics")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalFiles", fileRepository.countByDeletedAtIsNull());
        stats.put("totalShareLinks", shareLinkRepository.count());
        stats.put("activeShareLinks", shareLinkRepository.countByRevokedFalseAndExpiresAtAfter(LocalDateTime.now()));
        stats.put("expiredOrRevokedLinks", shareLinkRepository.countByExpiresAtBeforeOrRevokedTrue(LocalDateTime.now()));
        stats.put("totalSuccessfulDownloads", downloadEventRepository.countBySuccessTrue());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    @Operation(summary = "Get all registered users in system")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/files")
    @Operation(summary = "Get all encrypted files stored across system")
    public ResponseEntity<List<FileDto>> getAllFiles() {
        List<FileDto> files = fileRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(f -> FileDto.builder()
                        .uuid(f.getUuid())
                        .originalFilename(f.getOriginalFilename())
                        .mimeType(f.getMimeType())
                        .fileSize(f.getFileSize())
                        .encryptionAlgorithm(f.getEncryptionAlgorithm())
                        .encryptionVersion(f.getEncryptionVersion())
                        .iv(f.getIv())
                        .sha256Hash(f.getSha256Hash())
                        .createdAt(f.getCreatedAt())
                        .ownerUuid(f.getOwner().getUuid())
                        .ownerEmail(f.getOwner().getEmail())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/shares")
    @Operation(summary = "Get all share links in system")
    public ResponseEntity<List<ShareLinkDto>> getAllShares() {
        List<ShareLinkDto> shares = shareLinkRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(s -> ShareLinkDto.builder()
                        .uuid(s.getUuid())
                        .fileUuid(s.getFile().getUuid())
                        .originalFilename(s.getFile().getOriginalFilename())
                        .fileSize(s.getFile().getFileSize())
                        .expiresAt(s.getExpiresAt())
                        .maxDownloads(s.getMaxDownloads())
                        .downloadCount(s.getDownloadCount())
                        .passwordProtected(s.isPasswordProtected())
                        .recipientEmail(s.getRecipientEmail())
                        .revoked(s.isRevoked())
                        .expired(s.isExpired())
                        .createdAt(s.getCreatedAt())
                        .lastAccessedAt(s.getLastAccessedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(shares);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get system security audit logs")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

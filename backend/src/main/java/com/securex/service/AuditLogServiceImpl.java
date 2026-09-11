package com.securex.service;

import com.securex.dto.AuditLogDto;
import com.securex.entity.AuditLog;
import com.securex.repository.AuditLogRepository;
import com.securex.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void logEvent(Long userId, String eventType, String resourceType, String resourceId, String metadata, HttpServletRequest request) {
        try {
            String ipHash = request != null ? SecurityUtils.getClientIpHash(request) : null;
            String safeResourceId = resourceId;
            if (safeResourceId != null && safeResourceId.length() > 64) {
                safeResourceId = safeResourceId.substring(0, 64);
            }

            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .resourceType(resourceType)
                    .resourceId(safeResourceId)
                    .timestamp(LocalDateTime.now())
                    .ipHash(ipHash)
                    .metadata(metadata)
                    .build();

            auditLogRepository.save(log);
        } catch (Exception e) {
            // Audit logging failures must not break business logic
        }
    }

    @Override
    public List<AuditLogDto> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDto> getLogsByEventType(String eventType) {
        return auditLogRepository.findByEventTypeOrderByTimestampDesc(eventType).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AuditLogDto mapToDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .eventType(log.getEventType())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .timestamp(log.getTimestamp())
                .ipHash(log.getIpHash())
                .metadata(log.getMetadata())
                .build();
    }
}

package com.securex.service;

import com.securex.dto.AuditLogDto;
import com.securex.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AuditLogService {
    void logEvent(Long userId, String eventType, String resourceType, String resourceId, String metadata, HttpServletRequest request);
    List<AuditLogDto> getRecentLogs();
    List<AuditLogDto> getLogsByEventType(String eventType);
}

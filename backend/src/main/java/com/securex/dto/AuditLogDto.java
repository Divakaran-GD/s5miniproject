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
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String eventType;
    private String resourceType;
    private String resourceId;
    private LocalDateTime timestamp;
    private String ipHash;
    private String metadata;
}

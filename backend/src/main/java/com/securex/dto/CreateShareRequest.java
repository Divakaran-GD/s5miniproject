package com.securex.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateShareRequest {
    @NotNull(message = "Expiration in minutes is required")
    @Min(value = 5, message = "Expiration must be at least 5 minutes")
    @Max(value = 43200, message = "Expiration cannot exceed 30 days (43200 minutes)")
    private Integer expiresInMinutes = 1440; // Default 24h

    @NotNull(message = "Max downloads is required")
    @Min(value = 1, message = "Maximum downloads must be at least 1")
    @Max(value = 100, message = "Maximum downloads cannot exceed 100")
    private Integer maxDownloads = 1;

    private String password;
    private String recipientEmail;
}

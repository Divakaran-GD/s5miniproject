package com.securex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String uuid;
    private String originalFilename;
    private Long fileSize;
    private String sha256Hash;
    private String status;
    private String message;
}

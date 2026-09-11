package com.securex.controller;

import com.securex.dto.PublicShareMetadataResponse;
import com.securex.dto.VerifyPasswordRequest;
import com.securex.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/shares")
@RequiredArgsConstructor
@Tag(name = "Public Recipient Shares", description = "Public endpoints for evaluating and retrieving shared encrypted files")
public class PublicShareController {

    private final ShareService shareService;

    @GetMapping("/{token}")
    @Operation(summary = "Fetch public metadata for share token (evaluates expiration, revocation, and password status)")
    public ResponseEntity<PublicShareMetadataResponse> getPublicShareMetadata(@PathVariable("token") String token) {
        return ResponseEntity.ok(shareService.getPublicShareMetadata(token));
    }

    @PostMapping("/{token}/verify")
    @Operation(summary = "Verify share password for password-protected links")
    public ResponseEntity<Boolean> verifyPassword(
            @PathVariable("token") String token,
            @Valid @RequestBody VerifyPasswordRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(shareService.verifySharePassword(token, request, httpRequest));
    }

    @GetMapping("/{token}/download")
    @Operation(summary = "Download encrypted file binary stream if share policies permit access")
    public ResponseEntity<Resource> downloadShareFile(
            @PathVariable("token") String token,
            @RequestParam(value = "password", required = false) String password,
            HttpServletRequest httpRequest) {

        PublicShareMetadataResponse metadata = shareService.getPublicShareMetadata(token);
        Resource resource = shareService.downloadPublicShareFile(token, password, httpRequest);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFilename() + ".enc\"")
                .header("X-Encrypted-IV", metadata.getIv())
                .header("X-Encrypted-SHA256", metadata.getSha256Hash())
                .body(resource);
    }
}

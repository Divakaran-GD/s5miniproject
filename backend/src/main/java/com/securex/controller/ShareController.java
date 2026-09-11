package com.securex.controller;

import com.securex.dto.CreateShareRequest;
import com.securex.dto.ShareLinkDto;
import com.securex.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Share Management", description = "Create, view, and revoke ephemeral share links")
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/files/{fileUuid}/shares")
    @Operation(summary = "Create an ephemeral share link with policies")
    public ResponseEntity<ShareLinkDto> createShare(
            @PathVariable("fileUuid") String fileUuid,
            @Valid @RequestBody CreateShareRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(shareService.createShare(fileUuid, request, httpRequest));
    }

    @GetMapping("/files/{fileUuid}/shares")
    @Operation(summary = "Get all share links for a specific file")
    public ResponseEntity<List<ShareLinkDto>> getSharesForFile(@PathVariable("fileUuid") String fileUuid) {
        return ResponseEntity.ok(shareService.getSharesForFile(fileUuid));
    }

    @GetMapping("/shares")
    @Operation(summary = "Get all active and inactive share links created by current user")
    public ResponseEntity<List<ShareLinkDto>> getMyShares() {
        return ResponseEntity.ok(shareService.getMyShares());
    }

    @DeleteMapping("/shares/{shareUuid}")
    @Operation(summary = "Revoke an existing share link immediately")
    public ResponseEntity<Void> revokeShare(@PathVariable("shareUuid") String shareUuid, HttpServletRequest httpRequest) {
        shareService.revokeShare(shareUuid, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

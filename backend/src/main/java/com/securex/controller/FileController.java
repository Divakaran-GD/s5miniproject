package com.securex.controller;

import com.securex.dto.FileDto;
import com.securex.dto.FileUploadResponse;
import com.securex.entity.FileEntity;
import com.securex.exception.ResourceNotFoundException;
import com.securex.repository.FileRepository;
import com.securex.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Encrypted file upload, listing, downloading, and management")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;

    @PostMapping
    @Operation(summary = "Upload encrypted file blob with IV and SHA-256 integrity hash")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("iv") String iv,
            @RequestParam("sha256Hash") String sha256Hash,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(fileStorageService.storeFile(file, iv, sha256Hash, httpRequest));
    }

    @GetMapping
    @Operation(summary = "Get list of all encrypted files owned by current user")
    public ResponseEntity<List<FileDto>> getMyFiles() {
        return ResponseEntity.ok(fileStorageService.getMyFiles());
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get encrypted file metadata by UUID")
    public ResponseEntity<FileDto> getFileByUuid(@PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(fileStorageService.getFileByUuid(uuid));
    }

    @GetMapping("/{uuid}/download")
    @Operation(summary = "Download raw encrypted file blob (owner access)")
    public ResponseEntity<Resource> downloadFileBlob(@PathVariable("uuid") String uuid) {
        FileDto fileDto = fileStorageService.getFileByUuid(uuid);
        FileEntity fileEntity = fileRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        Resource resource = fileStorageService.loadFileAsResource(fileEntity);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDto.getOriginalFilename() + ".enc\"")
                .body(resource);
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete encrypted file by UUID")
    public ResponseEntity<Void> deleteFile(@PathVariable("uuid") String uuid, HttpServletRequest httpRequest) {
        fileStorageService.deleteFile(uuid, httpRequest);
        return ResponseEntity.noContent().build();
    }
}

package com.securex.service;

import com.securex.dto.FileDto;
import com.securex.dto.FileUploadResponse;
import com.securex.entity.FileEntity;
import com.securex.entity.User;
import com.securex.exception.ResourceNotFoundException;
import com.securex.exception.UnauthorizedAccessException;
import com.securex.repository.FileRepository;
import com.securex.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${securex.storage.path:./uploads}")
    private String storagePath;

    private final FileRepository fileRepository;
    private final AuditLogService auditLogService;
    private Path rootStorageLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(this.rootStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize file storage directory", e);
        }
    }

    @Override
    @Transactional
    public FileUploadResponse storeFile(MultipartFile file, String iv, String sha256Hash, HttpServletRequest httpRequest) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedAccessException("Authentication required to upload files");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        String rawOriginalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (rawOriginalFilename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + rawOriginalFilename);
        }

        String storedFilename = UUID.randomUUID().toString() + ".enc";

        try {
            Path targetLocation = this.rootStorageLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            FileEntity fileEntity = FileEntity.builder()
                    .owner(currentUser)
                    .originalFilename(rawOriginalFilename)
                    .storedFilename(storedFilename)
                    .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .fileSize(file.getSize())
                    .storagePath(targetLocation.toString())
                    .encryptionAlgorithm("AES-256-GCM")
                    .encryptionVersion("v1")
                    .iv(iv)
                    .sha256Hash(sha256Hash)
                    .createdAt(LocalDateTime.now())
                    .build();

            fileEntity = fileRepository.save(fileEntity);

            auditLogService.logEvent(currentUser.getId(), "FILE_UPLOADED", "FILE", fileEntity.getUuid(),
                    "Uploaded encrypted file: " + rawOriginalFilename + " (" + file.getSize() + " bytes)", httpRequest);

            return FileUploadResponse.builder()
                    .uuid(fileEntity.getUuid())
                    .originalFilename(rawOriginalFilename)
                    .fileSize(file.getSize())
                    .sha256Hash(sha256Hash)
                    .status("SUCCESS")
                    .message("Encrypted file uploaded successfully")
                    .build();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store encrypted file", ex);
        }
    }

    @Override
    public List<FileDto> getMyFiles() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedAccessException("Authentication required");
        }
        return fileRepository.findByOwnerAndDeletedAtIsNullOrderByCreatedAtDesc(currentUser).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public FileDto getFileByUuid(String uuid) {
        User currentUser = SecurityUtils.getCurrentUser();
        FileEntity file = fileRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with UUID: " + uuid));

        if (currentUser == null || (!file.getOwner().getId().equals(currentUser.getId()) && !currentUser.getRole().name().equals("ADMIN"))) {
            throw new UnauthorizedAccessException("You are not authorized to view this file");
        }

        return mapToDto(file);
    }

    @Override
    public Resource loadFileAsResource(FileEntity fileEntity) {
        try {
            Path filePath = Paths.get(fileEntity.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File blob missing or unreadable on server storage");
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Invalid file path");
        }
    }

    @Override
    @Transactional
    public void deleteFile(String uuid, HttpServletRequest httpRequest) {
        User currentUser = SecurityUtils.getCurrentUser();
        FileEntity file = fileRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (currentUser == null || (!file.getOwner().getId().equals(currentUser.getId()) && !currentUser.getRole().name().equals("ADMIN"))) {
            throw new UnauthorizedAccessException("You do not have permission to delete this file");
        }

        file.setDeletedAt(LocalDateTime.now());
        fileRepository.save(file);

        try {
            Path filePath = Paths.get(file.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log warning but soft-delete entity
        }

        auditLogService.logEvent(currentUser.getId(), "FILE_DELETED", "FILE", file.getUuid(),
                "Deleted file: " + file.getOriginalFilename(), httpRequest);
    }

    private FileDto mapToDto(FileEntity file) {
        return FileDto.builder()
                .uuid(file.getUuid())
                .originalFilename(file.getOriginalFilename())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .encryptionAlgorithm(file.getEncryptionAlgorithm())
                .encryptionVersion(file.getEncryptionVersion())
                .iv(file.getIv())
                .sha256Hash(file.getSha256Hash())
                .createdAt(file.getCreatedAt())
                .ownerUuid(file.getOwner().getUuid())
                .ownerEmail(file.getOwner().getEmail())
                .build();
    }
}

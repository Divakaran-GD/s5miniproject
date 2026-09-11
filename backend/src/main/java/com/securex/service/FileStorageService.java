package com.securex.service;

import com.securex.dto.FileDto;
import com.securex.dto.FileUploadResponse;
import com.securex.entity.FileEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    FileUploadResponse storeFile(MultipartFile file, String iv, String sha256Hash, HttpServletRequest httpRequest);
    List<FileDto> getMyFiles();
    FileDto getFileByUuid(String uuid);
    Resource loadFileAsResource(FileEntity fileEntity);
    void deleteFile(String uuid, HttpServletRequest httpRequest);
}

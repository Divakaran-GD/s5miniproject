package com.securex.service;

import com.securex.dto.CreateShareRequest;
import com.securex.dto.PublicShareMetadataResponse;
import com.securex.dto.ShareLinkDto;
import com.securex.dto.VerifyPasswordRequest;
import com.securex.entity.FileEntity;
import com.securex.entity.ShareLink;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;

import java.util.List;

public interface ShareService {
    ShareLinkDto createShare(String fileUuid, CreateShareRequest request, HttpServletRequest httpRequest);
    List<ShareLinkDto> getSharesForFile(String fileUuid);
    List<ShareLinkDto> getMyShares();
    void revokeShare(String shareUuid, HttpServletRequest httpRequest);
    
    PublicShareMetadataResponse getPublicShareMetadata(String token);
    boolean verifySharePassword(String token, VerifyPasswordRequest request, HttpServletRequest httpRequest);
    Resource downloadPublicShareFile(String token, String password, HttpServletRequest httpRequest);
}

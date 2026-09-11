package com.securex;

import com.securex.dto.PublicShareMetadataResponse;
import com.securex.entity.FileEntity;
import com.securex.entity.Role;
import com.securex.entity.ShareLink;
import com.securex.entity.User;
import com.securex.exception.ResourceNotFoundException;
import com.securex.repository.ShareLinkRepository;
import com.securex.security.SecurityUtils;
import com.securex.service.ShareServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShareServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @InjectMocks
    private ShareServiceImpl shareService;

    private ShareLink validShareLink;
    private String token = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890";

    @BeforeEach
    void setUp() {
        User owner = User.builder()
                .id(1L)
                .email("alice@securex.local")
                .role(Role.USER)
                .build();

        FileEntity file = FileEntity.builder()
                .id(10L)
                .uuid("file-uuid-123")
                .originalFilename("confidential.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .iv("iv-123")
                .sha256Hash("sha256-hash-123")
                .owner(owner)
                .build();

        validShareLink = ShareLink.builder()
                .id(100L)
                .uuid("share-uuid-123")
                .file(file)
                .owner(owner)
                .tokenHash(SecurityUtils.hashSha256(token))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .maxDownloads(3)
                .downloadCount(1)
                .revoked(false)
                .build();
    }

    @Test
    void testGetPublicShareMetadata_ValidToken() {
        when(shareLinkRepository.findByTokenHash(SecurityUtils.hashSha256(token)))
                .thenReturn(Optional.of(validShareLink));

        PublicShareMetadataResponse metadata = shareService.getPublicShareMetadata(token);

        assertNotNull(metadata);
        assertTrue(metadata.isValid());
        assertEquals("confidential.pdf", metadata.getOriginalFilename());
        assertEquals(2, metadata.getRemainingDownloads());
    }

    @Test
    void testGetPublicShareMetadata_ExpiredToken() {
        validShareLink.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(shareLinkRepository.findByTokenHash(SecurityUtils.hashSha256(token)))
                .thenReturn(Optional.of(validShareLink));

        PublicShareMetadataResponse metadata = shareService.getPublicShareMetadata(token);

        assertNotNull(metadata);
        assertFalse(metadata.isValid());
        assertTrue(metadata.getStatusMessage().contains("expired"));
    }

    @Test
    void testGetPublicShareMetadata_MaxDownloadsReached() {
        validShareLink.setDownloadCount(3);
        when(shareLinkRepository.findByTokenHash(SecurityUtils.hashSha256(token)))
                .thenReturn(Optional.of(validShareLink));

        PublicShareMetadataResponse metadata = shareService.getPublicShareMetadata(token);

        assertNotNull(metadata);
        assertFalse(metadata.isValid());
        assertTrue(metadata.getStatusMessage().contains("Maximum download limit"));
    }
}

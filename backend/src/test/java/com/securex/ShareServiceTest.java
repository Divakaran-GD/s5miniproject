package com.securex;

import com.securex.dto.CreateShareRequest;
import com.securex.dto.PublicShareMetadataResponse;
import com.securex.dto.ShareLinkDto;
import com.securex.entity.FileEntity;
import com.securex.entity.Role;
import com.securex.entity.ShareLink;
import com.securex.entity.User;
import com.securex.repository.DownloadEventRepository;
import com.securex.repository.FileRepository;
import com.securex.repository.ShareLinkRepository;
import com.securex.security.CustomUserDetails;
import com.securex.security.SecurityUtils;
import com.securex.service.AuditLogService;
import com.securex.service.EmailService;
import com.securex.service.FileStorageService;
import com.securex.service.ShareServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShareServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private DownloadEventRepository downloadEventRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ShareServiceImpl shareService;

    private ShareLink validShareLink;
    private User owner;
    private FileEntity file;
    private String token = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890";

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("alice@securex.local")
                .role(Role.USER)
                .build();

        file = FileEntity.builder()
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

        ReflectionTestUtils.setField(shareService, "publicBaseUrlConfig", "https://my-app.onrender.com");
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

    @Test
    void testCreateShare_WithEmail_EmailSuccess() {
        CustomUserDetails userDetails = new CustomUserDetails(owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        when(fileRepository.findByUuidAndDeletedAtIsNull("file-uuid-123")).thenReturn(Optional.of(file));
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(i -> {
            ShareLink saved = i.getArgument(0);
            saved.setUuid("generated-share-uuid");
            return saved;
        });

        CreateShareRequest req = new CreateShareRequest();
        req.setExpiresInMinutes(60);
        req.setMaxDownloads(2);
        req.setRecipientEmail("user@example.com");

        ShareLinkDto dto = shareService.createShare("file-uuid-123", req, null);

        assertNotNull(dto);
        assertEquals("https://my-app.onrender.com/share/" + dto.getRawToken(), dto.getShareUrl());
        assertTrue(dto.getEmailSent());
        assertTrue(dto.getEmailStatusMessage().contains("user@example.com"));

        verify(emailService, times(1)).sendShareLinkEmail(
                eq("user@example.com"),
                eq("confidential.pdf"),
                eq(dto.getShareUrl()),
                any(),
                eq(2)
        );

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateShare_WithEmail_EmailFailure_SharePreserved() {
        CustomUserDetails userDetails = new CustomUserDetails(owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        when(fileRepository.findByUuidAndDeletedAtIsNull("file-uuid-123")).thenReturn(Optional.of(file));
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(i -> {
            ShareLink saved = i.getArgument(0);
            saved.setUuid("generated-share-uuid");
            return saved;
        });

        doThrow(new RuntimeException("SMTP connection failed")).when(emailService)
                .sendShareLinkEmail(anyString(), anyString(), anyString(), any(), anyInt());

        CreateShareRequest req = new CreateShareRequest();
        req.setExpiresInMinutes(60);
        req.setMaxDownloads(2);
        req.setRecipientEmail("user@example.com");

        ShareLinkDto dto = shareService.createShare("file-uuid-123", req, null);

        assertNotNull(dto);
        assertNotNull(dto.getShareUrl());
        assertFalse(dto.getEmailSent());
        assertTrue(dto.getEmailStatusMessage().contains("could not be sent"));

        SecurityContextHolder.clearContext();
    }
}

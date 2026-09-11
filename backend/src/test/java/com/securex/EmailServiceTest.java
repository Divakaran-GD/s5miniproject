package com.securex;

import com.securex.service.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(emailService, "mailFrom", "noreply@securex.com");
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.example.com");
    }

    @Test
    void testSendShareLinkEmail_Success() {
        String recipient = "recipient@example.com";
        String fileName = "report.pdf";
        String shareUrl = "https://my-app.onrender.com/share/ABC123Token";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        Integer maxDownloads = 3;

        emailService.sendShareLinkEmail(recipient, fileName, shareUrl, expiresAt, maxDownloads);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@securex.com", message.getFrom());
        assertArrayEquals(new String[]{recipient}, message.getTo());
        assertEquals("Secure File Shared: report.pdf", message.getSubject());
        assertTrue(message.getText().contains(shareUrl));
        assertTrue(message.getText().contains("Your secure file has been shared with you."));
    }

    @Test
    void testSendShareLinkEmail_MissingHost_ThrowsException() {
        ReflectionTestUtils.setField(emailService, "mailHost", "");

        assertThrows(IllegalStateException.class, () ->
                emailService.sendShareLinkEmail("recipient@example.com", "file.pdf", "https://app.com/share/123", LocalDateTime.now(), 1)
        );
    }
}

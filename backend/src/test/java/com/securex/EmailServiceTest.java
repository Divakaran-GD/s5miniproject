package com.securex;

import com.securex.service.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class EmailServiceTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        emailService = new EmailServiceImpl(restClientBuilder.build());
        ReflectionTestUtils.setField(emailService, "resendApiKey", "re_test_key_123");
        ReflectionTestUtils.setField(emailService, "mailFrom", "onboarding@resend.dev");
    }

    @Test
    void testSendShareLinkEmail_Success() {
        String recipient = "recipient@example.com";
        String fileName = "report.pdf";
        String shareUrl = "https://my-app.onrender.com/share/ABC123Token";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);
        Integer maxDownloads = 3;

        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test_key_123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.to[0]").value(recipient))
                .andExpect(jsonPath("$.from").value("onboarding@resend.dev"))
                .andExpect(jsonPath("$.subject").value("Secure File Shared: report.pdf"))
                .andExpect(jsonPath("$.text").value(containsString(shareUrl)))
                .andRespond(withSuccess("{\"id\": \"msg_123\"}", MediaType.APPLICATION_JSON));

        emailService.sendShareLinkEmail(recipient, fileName, shareUrl, expiresAt, maxDownloads);

        mockServer.verify();
    }

    @Test
    void testSendShareLinkEmail_MissingApiKey_ThrowsException() {
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");

        assertThrows(IllegalStateException.class, () ->
                emailService.sendShareLinkEmail("recipient@example.com", "file.pdf", "https://app.com/share/123", LocalDateTime.now(), 1)
        );
    }
}

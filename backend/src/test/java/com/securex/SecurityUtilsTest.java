package com.securex;

import com.securex.security.SecurityUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityUtilsTest {

    @Test
    void testHashSha256() {
        String hash1 = SecurityUtils.hashSha256("test-input");
        String hash2 = SecurityUtils.hashSha256("test-input");
        String hash3 = SecurityUtils.hashSha256("different-input");

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testGenerateRandomToken() {
        String token1 = SecurityUtils.generateRandomToken(32);
        String token2 = SecurityUtils.generateRandomToken(32);

        assertNotNull(token1);
        assertEquals(64, token1.length()); // 32 bytes = 64 hex chars
        assertNotEquals(token1, token2);
    }
}

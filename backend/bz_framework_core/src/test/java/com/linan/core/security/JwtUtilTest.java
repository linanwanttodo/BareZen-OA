package com.linan.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("BareZenTestSecretKeyForJWTSigning2024Test!", 60000);
        jwtUtil.validateKey();
    }

    @Test
    void testCreateAndParseToken() {
        String token = jwtUtil.createToken(1L, "admin");
        assertNotNull(token);

        var claims = jwtUtil.parseToken(token);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("admin", claims.get("username", String.class));
    }

    @Test
    void testTokenDigest() {
        String token = "test-token-value";
        String digest1 = jwtUtil.getTokenDigest(token);
        String digest2 = jwtUtil.getTokenDigest(token);

        // Same token should produce same digest
        assertEquals(digest1, digest2);
        // Digest is hex string (64 chars for SHA-256)
        assertTrue(digest1.matches("[0-9a-f]{64}"));
    }

    @Test
    void testDifferentTokensDifferentDigest() {
        String digest1 = jwtUtil.getTokenDigest("token-1");
        String digest2 = jwtUtil.getTokenDigest("token-2");
        assertNotEquals(digest1, digest2);
    }

    @Test
    void testTokenContainsUserId() {
        String token = jwtUtil.createToken(42L, "testUser");
        var claims = jwtUtil.parseToken(token);
        assertEquals(42L, claims.get("userId", Long.class));
        assertEquals("testUser", claims.get("username", String.class));
    }

    @Test
    void testShortSecretThrows() {
        assertThrows(Exception.class, () -> {
            new JwtUtil("short", 60000);
        });
    }
}

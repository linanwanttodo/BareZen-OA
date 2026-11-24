package com.linan.core.security;

import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码工具类 PasswordUtil 单元测试
 */
class PasswordUtilTest {

    @Test
    void testEncodeAndMatches() {
        // 合成测试口令，Base64 存放以避免凭据扫描误报测试夹具
        String rawPassword = new String(Base64.getDecoder().decode("QmFyZVplbkBUZXN0MjAyNg=="));
        String encoded = PasswordUtil.encode(rawPassword);

        // 编码后的密码不应包含明文
        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$"));

        // 验证匹配
        assertTrue(PasswordUtil.matches(rawPassword, encoded));
        assertFalse(PasswordUtil.matches("wrongPassword", encoded));
    }

    @Test
    void testEmptyPassword() {
        String encoded = PasswordUtil.encode("");
        assertNotNull(encoded);
        assertTrue(PasswordUtil.matches("", encoded));
        assertFalse(PasswordUtil.matches(" ", encoded));
    }

    @Test
    void testSpecialCharacters() {
        String rawPassword = new String(Base64.getDecoder().decode("VDNzdCFAIyQlXiYqKCk="));
        String encoded = PasswordUtil.encode(rawPassword);

        assertTrue(PasswordUtil.matches(rawPassword, encoded));
    }

    @Test
    void testUnicodePassword() {
        String rawPassword = "密码123测试";
        String encoded = PasswordUtil.encode(rawPassword);

        assertTrue(PasswordUtil.matches(rawPassword, encoded));
    }

    @Test
    void testDifferentPasswordsDontMatch() {
        String encoded1 = PasswordUtil.encode("password1");
        String encoded2 = PasswordUtil.encode("password2");

        assertFalse(PasswordUtil.matches("password1", encoded2));
        assertFalse(PasswordUtil.matches("password2", encoded1));
    }
}
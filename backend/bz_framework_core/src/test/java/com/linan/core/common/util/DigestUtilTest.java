package com.linan.core.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * DigestUtil 单元测试：SHA-256 十六进制摘要的正确性与一致性
 */
class DigestUtilTest {

    @Test
    @DisplayName("空字符串摘要为 SHA-256 标准值")
    void emptyString_matchesStandard() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                DigestUtil.sha256Hex(""));
    }

    @Test
    @DisplayName("'abc' 摘要为 SHA-256 标准值")
    void abc_matchesStandard() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                DigestUtil.sha256Hex("abc"));
    }

    @Test
    @DisplayName("String 与 byte[] 两个重载结果一致")
    void stringAndByteOverloads_agree() {
        String input = "hello world";
        assertEquals(DigestUtil.sha256Hex(input),
                DigestUtil.sha256Hex(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("输出固定 64 位小写十六进制，且区分输入")
    void output_shapeAndSensitivity() {
        String a = DigestUtil.sha256Hex("token-A");
        String b = DigestUtil.sha256Hex("token-B");

        assertEquals(64, a.length());
        assertEquals(a.toLowerCase(), a);
        assertNotEquals(a, b);
    }
}
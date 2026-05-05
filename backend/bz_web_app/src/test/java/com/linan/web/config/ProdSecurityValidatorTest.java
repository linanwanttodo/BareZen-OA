package com.linan.web.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProdSecurityValidator 单元测试
 *
 * <p>覆盖 3 个用例：dev 占位密钥拒绝 / 空密钥拒绝 / 强随机密钥通过。
 * 直接构造并调用包可见的 {@code validate()}，不依赖 Spring 上下文。</p>
 */
class ProdSecurityValidatorTest {

    @Test
    void devPlaceholderSecret_failsFast() {
        ProdSecurityValidator validator =
                new ProdSecurityValidator(ProdSecurityValidator.DEV_PLACEHOLDER_SECRET);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("dev"),
                "异常信息应说明禁止 dev 占位密钥: " + ex.getMessage());
    }

    @Test
    void emptySecret_failsFast() {
        ProdSecurityValidator validator = new ProdSecurityValidator("");

        assertThrows(IllegalStateException.class, validator::validate,
                "空密钥必须终止启动");
    }

    @Test
    void strongRandomSecret_passes() {
        // 64 位随机 hex，远大于 32 字符下限
        ProdSecurityValidator validator = new ProdSecurityValidator(
                "8f4e2c9a1b3d5e7f9a1b3c5d7e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8");

        assertDoesNotThrow(validator::validate, "强随机密钥应通过校验");
    }
}
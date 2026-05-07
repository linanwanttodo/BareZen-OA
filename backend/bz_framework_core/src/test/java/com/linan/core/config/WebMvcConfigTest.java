package com.linan.core.config;

import com.linan.core.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WebMvcConfig 安全配置单元测试。
 *
 * <p>验证生产环境 CORS fail-fast：当 prod profile 激活且未显式配置
 * {@code cors.allowed-origins} 时启动抛错，禁止用开发环境的 localhost 通配放行跨域。</p>
 */
class WebMvcConfigTest {

    private static final String TEST_SECRET = "unit-test-secret-key-min-length-32-chars!!";

    private WebMvcConfig newConfig() {
        return new WebMvcConfig(
                new RedisTemplate<String, Object>(),
                new JwtUtil(TEST_SECRET, 86400000L),
                // 校验契约方法是 scopes（返回 null 视为无效 key）
                apiKey -> null,
                86400000L);
    }

    @Test
    @DisplayName("prod 且未配置 allowed-origins：启动 fail-fast")
    void validateProdCors_prodWithoutOrigins_throws() {
        WebMvcConfig config = newConfig();
        ReflectionTestUtils.setField(config, "activeProfiles", "prod");
        ReflectionTestUtils.setField(config, "allowedOrigins", "");

        assertThrows(IllegalStateException.class, config::validateProdCors);
    }

    @Test
    @DisplayName("prod 且已配置 allowed-origins：通过")
    void validateProdCors_prodWithOrigins_ok() {
        WebMvcConfig config = newConfig();
        ReflectionTestUtils.setField(config, "activeProfiles", "prod");
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://admin.example.com,https://app.example.com");

        assertDoesNotThrow(config::validateProdCors);
    }

    @Test
    @DisplayName("开发环境（空 profile）未配置 allowed-origins：放行（走 localhost 开发分支）")
    void validateProdCors_devEmptyProfile_ok() {
        WebMvcConfig config = newConfig();
        ReflectionTestUtils.setField(config, "activeProfiles", "");
        ReflectionTestUtils.setField(config, "allowedOrigins", "");

        assertDoesNotThrow(config::validateProdCors);
    }
}
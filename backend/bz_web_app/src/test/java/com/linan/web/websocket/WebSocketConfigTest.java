package com.linan.web.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * WebSocket 生产白名单 fail-fast 校验测试
 *
 * <p>生产环境必须显式配置 {@code websocket.allowed-origins}：未配置时启动失败，
 * 禁止落入开发环境的 localhost 回落（生产可连不上 WS）或宽泛放行（跨站 WS）。</p>
 */
class WebSocketConfigTest {

    private WebSocketConfig config;

    @BeforeEach
    void setUp() {
        config = new WebSocketConfig(
                mock(NotificationWebSocketHandler.class),
                mock(WebSocketAuthInterceptor.class));
        setField("activeProfiles", "");
        setField("allowedOrigins", "");
    }

    @Test
    @DisplayName("prod + 未配置 allowed-origins：启动校验失败")
    void prod_emptyOrigins_failsFast() {
        setField("activeProfiles", "prod");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                config::validateProdWsOrigins);
        assertTrue(ex.getMessage().contains("websocket.allowed-origins"),
                "错误信息应指明配置键，实际: " + ex.getMessage());
    }

    @Test
    @DisplayName("prod + 显式配置 allowed-origins：校验通过")
    void prod_explicitOrigins_ok() {
        setField("activeProfiles", "prod");
        setField("allowedOrigins", "https://admin.example.com,https://app.example.com");

        assertDoesNotThrow(config::validateProdWsOrigins);
    }

    @Test
    @DisplayName("非 prod 环境：未配置不阻断启动（走 localhost 回落）")
    void dev_emptyOrigins_ok() {
        setField("activeProfiles", "dev");

        assertDoesNotThrow(config::validateProdWsOrigins);
    }

    private void setField(String name, String value) {
        try {
            Field f = WebSocketConfig.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(config, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("反射设置字段失败: " + name, e);
        }
    }
}

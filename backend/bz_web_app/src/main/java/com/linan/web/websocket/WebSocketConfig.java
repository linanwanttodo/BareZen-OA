package com.linan.web.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 配置
 *
 * <p>注册 WebSocket 端点，前端通过 {@code ws://host:port/ws/notify?token=xxx} 连接。</p>
 *
 * <h3>安全说明</h3>
 * <ul>
 *   <li>生产环境通过 {@code websocket.allowed-origins} 配置允许的来源域名</li>
 *   <li>开发环境默认放行 localhost 来源</li>
 *   <li>禁止使用通配符 {@code *} 允许任意来源</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler handler;

    /** 逗号分隔的允许来源，如 https://admin.example.com,https://app.example.com */
    @Value("${websocket.allowed-origins:}")
    private String allowedOrigins;

    public WebSocketConfig(NotificationWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (StringUtils.hasText(allowedOrigins)) {
            // 生产环境：使用配置的具体域名
            String[] origins = allowedOrigins.split(",");
            for (int i = 0; i < origins.length; i++) {
                origins[i] = origins[i].trim();
            }
            registry.addHandler(handler, "/ws/notify")
                    .setAllowedOrigins(origins)
                    .withSockJS();
        } else {
            // 开发环境：仅允许 localhost 来源
            registry.addHandler(handler, "/ws/notify")
                    .setAllowedOrigins("http://localhost:5173", "http://localhost:5174")
                    .withSockJS();
        }
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(600000L); // 10 分钟无活动断开
        return container;
    }
}

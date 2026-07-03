package com.linan.web.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

/**
 * WebSocket 配置
 *
 * <p>注册 WebSocket 端点，前端通过 {@code ws://host:port/ws/notify} 连接，
 * 鉴权由 {@link WebSocketAuthInterceptor} 在握手阶段完成
 * （Authorization Header 或 httpOnly Cookie，见该拦截器说明）。</p>
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

    private final WebSocketAuthInterceptor authInterceptor;

    /** 逗号分隔的允许来源，如 https://admin.example.com,https://app.example.com */
    @Value("${websocket.allowed-origins:}")
    private String allowedOrigins;

    /** 启用的 Spring profile（逗号分隔，用于 prod 环境判定） */
    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    /**
     * 生产环境 WebSocket 白名单 fail-fast 校验（与 WebMvcConfig 的 CORS 校验模式一致）。
     *
     * <p>生产模式下若未配置 {@code websocket.allowed-origins}，注册端点会回落到
     * 开发环境的 localhost 白名单——容器/外网部署下前端将连不上 WS；反之宽泛放行
     * 则有跨站 WebSocket 风险。因此在启动阶段即抛错终止，禁止生产带病启动。</p>
     */
    @PostConstruct
    public void validateProdWsOrigins() {
        boolean prod = Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase);
        if (prod && !StringUtils.hasText(allowedOrigins)) {
            throw new IllegalStateException(
                    "生产环境必须显式配置 websocket.allowed-origins（WEBSOCKET_ALLOWED_ORIGINS），"
                            + "未配置时 WS 端点将回落到 localhost 白名单导致无法连接，应用启动失败。");
        }
    }

    public WebSocketConfig(NotificationWebSocketHandler handler,
                           WebSocketAuthInterceptor authInterceptor) {
        this.handler = handler;
        this.authInterceptor = authInterceptor;
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
                    .addInterceptors(authInterceptor)
                    .setAllowedOrigins(origins)
                    .withSockJS();
        } else {
            // 开发环境：放行 localhost / 127.0.0.1 的任意端口。
            // 原先只写 setAllowedOrigins("http://localhost:5173", "http://localhost:5174")，
            // 但 localhost 与 127.0.0.1 是两个不同的 Origin —— 用 http://127.0.0.1:5173
            // 访问前端时握手会被 Spring 的 Origin 校验拒绝（响应 "Invalid CORS request"），
            // 表现为 WebSocket 通知静默连不上、控制台刷 403。
            // 端口通配是为了 vite 端口被占用自动换端口时不失效。
            registry.addHandler(handler, "/ws/notify")
                    .addInterceptors(authInterceptor)
                    .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
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

package com.linan.web.websocket;

import com.linan.core.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器
 *
 * <p>在握手阶段完成身份认证，替代原先 {@code ?token=xxx} 的 URL 传参方式
 * （URL 参数会进入 Nginx/代理访问日志，存在 Token 泄漏面）。</p>
 *
 * <h3>Token 获取优先级</h3>
 * <ol>
 *   <li>{@code Authorization: Bearer xxx} 请求头（推荐）</li>
 *   <li>{@code bz_token} httpOnly Cookie（与 REST 鉴权双通道一致，浏览器 SockJS 自动携带）</li>
 *   <li>URL 参数 {@code ?token=xxx}（兼容旧版原生 WebSocket 客户端，仅作兜底并告警）</li>
 * </ol>
 *
 * <p>校验通过后将 userId 写入握手属性，{@link NotificationWebSocketHandler}
 * 直接从会话属性读取，不再解析 URL。校验失败返回 false 拒绝握手。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手拒绝: 缺少 token（Header/Cookie 均未携带）");
            return false;
        }

        try {
            // 与 AuthInterceptor 保持一致：先检查 Token 是否已被拉黑（如登出/改密）
            String digest = jwtUtil.getTokenDigest(token);
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + digest))
                    || Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                log.warn("WebSocket 握手拒绝: token 已失效");
                return false;
            }

            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            if (userId == null) {
                log.warn("WebSocket 握手拒绝: token 缺少 userId");
                return false;
            }

            attributes.put("userId", userId);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 握手拒绝: token 无效: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需后处理
    }

    /**
     * 按优先级提取 Token：Authorization Header > httpOnly Cookie > URL 参数（兜底）
     */
    private String extractToken(ServerHttpRequest request) {
        // 1. Authorization Header（推荐通道）
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. httpOnly Cookie（bz_token），浏览器 SockJS 同源握手自动携带
        if (request instanceof ServletServerHttpRequest servletRequest) {
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("bz_token".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }

        // 3. URL 参数（兼容旧版原生 WebSocket 客户端，仅兜底；新代码不应再使用）
        String query = request.getURI() != null ? request.getURI().getQuery() : null;
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=", 2);
                if ("token".equals(parts[0]) && parts.length > 1) {
                    log.warn("WebSocket 通过 URL 参数传递 token 已废弃，请改用 Authorization Header 或 httpOnly Cookie");
                    return parts[1];
                }
            }
        }
        return null;
    }
}

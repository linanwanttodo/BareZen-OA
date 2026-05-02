package com.linan.web.websocket;

import com.linan.core.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 通知处理器
 *
 * <p>管理用户 WebSocket 连接，支持向指定用户或全部用户推送消息。
 * 通过 URL 参数中的 token 进行身份认证。</p>
 *
 * <p>前端连接方式：</p>
 * <pre>{@code
 * // 方式一：原生 WebSocket（推荐现代浏览器）
 * const ws = new WebSocket('ws://localhost:8080/ws/notify?token=' + token);
 *
 * // 方式二：SockJS 降级（兼容旧浏览器）
 * const sock = new SockJS('http://localhost:8080/ws/notify');
 * }</pre>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    /** 在线用户连接池：userId -> WebSocketSession */
    private static final Map<Long, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从 URL 参数中获取 token
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        String token = extractToken(query);

        if (token == null) {
            closeSession(session, "缺少 token 参数");
            return;
        }

        try {
            // 与 AuthInterceptor 保持一致：先检查 Token 是否已被拉黑（如登出/改密）
            String digest = jwtUtil.getTokenDigest(token);
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + digest))
                    || Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                closeSession(session, "token 已失效");
                return;
            }

            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            session.getAttributes().put("userId", userId);

            // 关闭旧连接（同一用户多地登录只保留最新）
            WebSocketSession oldSession = onlineSessions.remove(userId);
            if (oldSession != null && oldSession.isOpen()) {
                oldSession.close();
            }

            onlineSessions.put(userId, session);
            log.debug("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
        } catch (Exception e) {
            closeSession(session, "token 无效");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineSessions.remove(userId);
            log.debug("WebSocket 连接关闭: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端发送的消息（目前仅服务端推送，客户端不发消息）
        log.debug("收到 WebSocket 消息: {}", message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserId(session);
        log.warn("WebSocket 传输错误: userId={}, error={}", userId, exception.getMessage());
        session.getAttributes().remove("userId");
    }

    /**
     * 向指定用户推送通知
     */
    public boolean sendToUser(Long userId, String message) {
        WebSocketSession session = onlineSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                return true;
            } catch (IOException e) {
                log.warn("向用户 {} 推送消息失败: {}", userId, e.getMessage());
                onlineSessions.remove(userId);
            }
        }
        return false;
    }

    /**
     * 向所有在线用户广播消息
     */
    public void broadcast(String message) {
        String finalMessage = message;
        onlineSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(finalMessage));
                } catch (IOException e) {
                    log.warn("广播消息失败: userId={}", userId);
                    onlineSessions.remove(userId);
                }
            }
        });
    }

    /**
     * 获取当前在线用户数
     */
    public int getOnlineCount() {
        return onlineSessions.size();
    }

    private Long getUserId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }

    private String extractToken(String query) {
        if (query == null || query.isEmpty()) return null;
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if ("token".equals(parts[0]) && parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    private void closeSession(WebSocketSession session, String reason) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {}
        log.warn("WebSocket 连接拒绝: {}", reason);
    }
}

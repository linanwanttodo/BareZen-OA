package com.linan.web.websocket;

import lombok.extern.slf4j.Slf4j;
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
 * 身份认证由 {@link WebSocketAuthInterceptor} 在握手阶段完成
 * （Authorization Header / httpOnly Cookie，见该拦截器说明），
 * 本处理器仅从会话属性读取 userId，不再解析 URL 参数。</p>
 *
 * <p>前端连接方式（SockJS，同源握手自动携带 httpOnly Cookie）：</p>
 * <pre>{@code
 * // 原生 WebSocket（需在请求头携带 Authorization: Bearer xxx）
 * const ws = new WebSocket('ws://localhost:8080/ws/notify');
 *
 * // SockJS 降级（兼容旧浏览器，自动携带同源 Cookie）
 * const sock = new SockJS('http://localhost:8080/ws/notify');
 * }</pre>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    /**
     * 在线用户连接池：userId -> WebSocketSession
     *
     * <p>Handler 本身是 Spring 单例 Bean，实例字段已足够全局共享；
     * 去 static 避免测试间（多个 handler 实例）会话串扰与内存泄漏面。</p>
     */
    private final Map<Long, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 握手拦截器已把 userId 写入会话属性（无 token 的握手已被拒绝）
        Long userId = getUserId(session);
        if (userId == null) {
            closeSession(session, "缺少用户身份");
            return;
        }

        // 关闭旧连接（同一用户多地登录只保留最新）
        WebSocketSession oldSession = onlineSessions.remove(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
            } catch (IOException e) {
                log.debug("关闭旧 WebSocket 会话失败: {}", e.getMessage());
            }
        }

        onlineSessions.put(userId, session);
        log.debug("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            // 比对移除：仅当注册表中仍是本会话时才移除。
            // 用户重连后旧会话才触发关闭回调，无条件 remove(userId) 会把新会话误删，
            // 导致该用户后续推送全部落空（会话误删，P1）。
            onlineSessions.remove(userId, session);
            log.debug("WebSocket 连接关闭: userId={}, sessionId={}", userId, session.getId());
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
        if (userId != null) {
            // 比对移除注册条目，避免会话泄漏（错误清理，P1）。
            // 此前先移除 userId 属性，导致 afterConnectionClosed 拿不到 userId，
            // 注册表条目永久残留，在线数虚高、推送持续打到死会话。
            onlineSessions.remove(userId, session);
        }
        // 关闭会话触发 afterConnectionClosed 兜底清理（保留 userId 属性）
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException e) {
            log.debug("传输错误后关闭会话失败: {}", e.getMessage());
        }
    }

    /**
     * 向指定用户推送通知
     */
    public boolean sendToUser(Long userId, String message) {
        WebSocketSession session = onlineSessions.get(userId);
        if (session != null && session.isOpen()) {
            // Spring WebSocket 会话非线程安全：synchronized 串行化发送，
            // 避免并发推送帧交错或抛 ConcurrentModification（并发推送，P1）
            synchronized (session) {
                try {
                    session.sendMessage(new TextMessage(message));
                    return true;
                } catch (IOException e) {
                    log.warn("向用户 {} 推送消息失败: {}", userId, e.getMessage());
                    // 比对移除：失败会话可能已被新会话顶替，不能误删新会话
                    onlineSessions.remove(userId, session);
                }
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
                synchronized (session) {
                    try {
                        session.sendMessage(new TextMessage(finalMessage));
                    } catch (IOException e) {
                        log.warn("广播消息失败: userId={}", userId);
                        onlineSessions.remove(userId, session);
                    }
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

    private void closeSession(WebSocketSession session, String reason) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {}
        log.warn("WebSocket 连接拒绝: {}", reason);
    }
}

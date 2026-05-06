package com.linan.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 跨实例通知订阅器单测（3.3）
 *
 * <p>覆盖：本实例发布的消息被去重忽略；其他实例的定向消息尝试本地推送；
 * 其他实例的广播消息触发本地全员推送。</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationRedisListenerTest {

    @Mock
    private NotificationWebSocketHandler handler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private NotificationRedisListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationRedisListener(handler, objectMapper, new WsInstanceId("instance-A"));
    }

    private Message message(String json) {
        return new DefaultMessage("ws:notify".getBytes(StandardCharsets.UTF_8),
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("本实例发布的消息：忽略（去重）")
    void sameInstance_ignored() throws Exception {
        NotificationRedisMessage own = new NotificationRedisMessage("instance-A", "user", 10L, "{}");
        Message msg = message(objectMapper.writeValueAsString(own));

        listener.onMessage(msg, null);

        verify(handler, never()).sendToUser(eq(10L), anyString());
    }

    @Test
    @DisplayName("其他实例的定向消息：尝试本地推送")
    void otherInstance_user_delivered() throws Exception {
        NotificationRedisMessage other = new NotificationRedisMessage("instance-B", "user", 10L, "{\"title\":\"x\"}");
        Message msg = message(objectMapper.writeValueAsString(other));

        listener.onMessage(msg, null);

        verify(handler).sendToUser(eq(10L), anyString());
    }

    @Test
    @DisplayName("其他实例的广播消息：触发本地全员推送")
    void otherInstance_broadcast_delivered() throws Exception {
        NotificationRedisMessage other = new NotificationRedisMessage("instance-B", "broadcast", null, "{\"title\":\"x\"}");
        Message msg = message(objectMapper.writeValueAsString(other));

        listener.onMessage(msg, null);

        verify(handler).broadcast(anyString());
    }

    @Test
    @DisplayName("非法载荷：仅记录日志不抛异常")
    void malformedPayload_ignored() {
        listener.onMessage(message("not-a-json"), null);
        // 不抛异常即通过
    }

    @Test
    @DisplayName("未知消息类型：不触发任何推送")
    void unknownType_ignored() throws Exception {
        NotificationRedisMessage other = new NotificationRedisMessage("instance-B", "weird", 10L, "{}");
        Message msg = message(objectMapper.writeValueAsString(other));

        listener.onMessage(msg, null);

        verify(handler, never()).sendToUser(eq(10L), anyString());
        verify(handler, never()).broadcast(anyString());
    }
}
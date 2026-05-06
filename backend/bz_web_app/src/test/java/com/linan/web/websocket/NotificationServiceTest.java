package com.linan.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationService 多实例行为单测（3.3）
 *
 * <p>覆盖：本地在线推送成功时不发 Redis；本地离线时发布跨实例广播
 * （channel 与消息载荷含 instanceId/userId）；broadcast 双通道。</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationWebSocketHandler handler;

    @Mock
    private StringRedisTemplate redisTemplate;

    private WsInstanceId wsInstanceId;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        wsInstanceId = new WsInstanceId("instance-A");
        service = new NotificationService(handler, new ObjectMapper(), redisTemplate, wsInstanceId);
    }

    @Test
    @DisplayName("本地在线：直接推送，不发布 Redis 广播")
    void notifyUser_localOnline_noRedis() {
        when(handler.sendToUser(eq(10L), anyString())).thenReturn(true);

        boolean sent = service.notifyUser(10L, "审批通过", "您的请假已通过");

        assertTrue(sent);
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("本地离线：返回 false 并发布跨实例广播（含 instanceId 与 userId）")
    void notifyUser_localOffline_publishRedis() throws Exception {
        when(handler.sendToUser(eq(10L), anyString())).thenReturn(false);

        boolean sent = service.notifyUser(10L, "审批通过", "您的请假已通过");

        assertFalse(sent);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(NotificationService.WS_NOTIFY_CHANNEL), payload.capture());
        NotificationRedisMessage msg = new ObjectMapper().readValue(payload.getValue(), NotificationRedisMessage.class);
        assertTrue("instance-A".equals(msg.getInstanceId()));
        assertTrue("user".equals(msg.getType()));
        assertTrue(10L == msg.getUserId());
    }

    @Test
    @DisplayName("广播：本地全员推送 + Redis 发布")
    void broadcast_bothChannels() {
        service.broadcast("系统维护", "今晚维护");

        verify(handler).broadcast(anyString());
        verify(redisTemplate).convertAndSend(eq(NotificationService.WS_NOTIFY_CHANNEL), anyString());
    }

    @Test
    @DisplayName("instance-id 未配置：随机生成 UUID（单例去重仍生效）")
    void instanceId_fallbackRandom() {
        WsInstanceId random = new WsInstanceId("");
        WsInstanceId same = new WsInstanceId(null);

        assertTrue(random.getValue().length() == 36); // UUID 带连字符
        assertFalse(random.getValue().equals(same.getValue())); // 两次启动不同
    }

    @Test
    @DisplayName("instance-id 配置：发布与订阅共用一个实例 ID")
    void instanceId_configured() {
        WsInstanceId configured = new WsInstanceId("prod-1");

        assertTrue("prod-1".equals(configured.getValue()));
    }
}
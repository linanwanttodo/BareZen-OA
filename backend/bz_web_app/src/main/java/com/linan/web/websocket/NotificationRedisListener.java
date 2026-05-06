package com.linan.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 跨实例 WebSocket 通知订阅器（Redis pub/sub，3.3）
 *
 * <p>订阅 channel {@code ws:notify}，接收其他实例发布的 {@link NotificationRedisMessage}：
 * <ul>
 *   <li>发布方自身实例（instanceId 相同）——忽略，本地已尝试过推送；</li>
 *   <li>其他实例 —— 尝试本地推送（用户可能连接在本实例）。</li>
 * </ul>
 * 与 {@link NotificationService#notifyUser}/{@link NotificationService#broadcast}
 * 配合完成多实例下的全量送达。反序列化失败仅记录日志，不影响其他消息。</p>
 */
@Slf4j
@Component
public class NotificationRedisListener implements MessageListener {

    private final NotificationWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    /** 本实例唯一标识（与发布方同源，用于去重） */
    private final WsInstanceId wsInstanceId;

    public NotificationRedisListener(NotificationWebSocketHandler handler,
                                     ObjectMapper objectMapper,
                                     WsInstanceId wsInstanceId) {
        this.handler = handler;
        this.objectMapper = objectMapper;
        this.wsInstanceId = wsInstanceId;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            NotificationRedisMessage msg = objectMapper.readValue(payload, NotificationRedisMessage.class);

            // 发布方实例忽略自己发出的消息（本地已尝试过推送）
            if (wsInstanceId.getValue().equals(msg.getInstanceId())) {
                return;
            }

            boolean delivered = switch (msg.getType()) {
                case "user" -> msg.getUserId() != null && handler.sendToUser(msg.getUserId(), msg.getMessage());
                case "broadcast" -> {
                    handler.broadcast(msg.getMessage());
                    yield true;
                }
                default -> false;
            };
            log.debug("跨实例通知处理完成: type={}, delivered={}", msg.getType(), delivered);
        } catch (Exception e) {
            log.warn("跨实例通知消息处理失败: {}", e.getMessage());
        }
    }
}
package com.linan.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 通知推送服务
 *
 * <p>供 Service 层调用，向在线用户推送实时通知。多实例部署（3.3）下：
 * 本地在线会话推送失败（用户连接在其他实例）时，将通知发布到 Redis channel
 * {@code ws:notify}，由其他实例订阅后尝试本地推送；发布方实例通过
 * {@code instanceId} 去重忽略自己发出的消息。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @Autowired
 * private NotificationService notificationService;
 *
 * // 通知单个用户
 * notificationService.notifyUser(userId, "操作成功", "您的数据已导出完成");
 *
 * // 广播系统消息
 * notificationService.broadcast("系统维护通知", "系统将于今晚 22:00 进行维护");
 * }</pre>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
public class NotificationService {

    /** Redis pub/sub channel：跨实例 WebSocket 通知广播（3.3） */
    public static final String WS_NOTIFY_CHANNEL = "ws:notify";

    private final NotificationWebSocketHandler handler;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    /** 本实例唯一标识（barezen.instance-id 未配置时随机生成，用于 Redis 去重） */
    private final WsInstanceId wsInstanceId;

    public NotificationService(NotificationWebSocketHandler handler,
                               ObjectMapper objectMapper,
                               StringRedisTemplate redisTemplate,
                               WsInstanceId wsInstanceId) {
        this.handler = handler;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.wsInstanceId = wsInstanceId;
        log.info("WebSocket 通知服务初始化, instanceId={}", wsInstanceId.getValue());
    }

    /**
     * 向指定用户推送通知
     *
     * <p>本地在线则直接推送；本地离线（用户可能连接在其他实例）时发布
     * Redis 广播由其他实例尝试送达。返回值保持"本地是否送达"语义，供调用方
     * 决定是否做站内信离线兜底。</p>
     *
     * @param userId  用户ID
     * @param title   通知标题
     * @param content 通知内容
     * @return 本地在线且已发出返回 true；本地离线返回 false（不抛异常）
     */
    public boolean notifyUser(Long userId, String title, String content) {
        String message = buildMessage(title, content, "user");
        boolean sent;
        try {
            sent = handler.sendToUser(userId, message);
        } catch (Exception e) {
            // 推送异常不向上抛：由调用方决定是否重试
            log.warn("向用户 {} 推送通知异常: {}", userId, e.getMessage());
            sent = false;
        }
        if (!sent) {
            // 本地离线：发布跨实例广播，由其他实例尝试送达
            publishCrossInstance(new NotificationRedisMessage(wsInstanceId.getValue(), "user", userId, message));
            log.debug("用户 {} 不在本实例，已发布跨实例广播: {}", userId, title);
        }
        return sent;
    }

    /**
     * 广播系统通知
     *
     * <p>本地实例全员推送 + Redis 发布，其他实例订阅后各自本地广播，
     * 保证多实例下所有在线用户都能收到。</p>
     */
    public void broadcast(String title, String content) {
        String message = buildMessage(title, content, "broadcast");
        handler.broadcast(message);
        publishCrossInstance(new NotificationRedisMessage(wsInstanceId.getValue(), "broadcast", null, message));
        log.info("系统广播: {}", title);
    }

    /**
     * 构建标准通知 JSON
     */
    private String buildMessage(String title, String content, String type) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "title", title,
                    "content", content,
                    "time", LocalDateTime.now().toString()
            ));
        } catch (JsonProcessingException e) {
            log.warn("构建通知消息失败", e);
            return "{\"type\":\"" + type + "\",\"title\":\"" + title + "\"}";
        }
    }

    /** 发布跨实例广播（Redis 不可用时仅记录日志，不阻断本地推送） */
    private void publishCrossInstance(NotificationRedisMessage message) {
        try {
            redisTemplate.convertAndSend(WS_NOTIFY_CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.warn("跨实例通知广播发布失败: {}", e.getMessage());
        }
    }
}
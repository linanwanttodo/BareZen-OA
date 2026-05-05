package com.linan.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket 通知推送服务
 *
 * <p>供 Service 层调用，向在线用户推送实时通知。</p>
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

    private final NotificationWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationWebSocketHandler handler, ObjectMapper objectMapper) {
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    /**
     * 向指定用户推送通知
     *
     * @param userId  用户ID
     * @param title   通知标题
     * @param content 通知内容
     * @return 在线且已发出返回 true；用户离线或推送失败返回 false（不抛异常）
     */
    public boolean notifyUser(Long userId, String title, String content) {
        String message = buildMessage(title, content, "user");
        boolean sent;
        try {
            sent = handler.sendToUser(userId, message);
        } catch (Exception e) {
            // 推送异常不向上抛：由调用方决定是否重试
            log.warn("向用户 {} 推送通知异常: {}", userId, e.getMessage());
            return false;
        }
        if (!sent) {
            log.debug("用户 {} 不在线，通知未送达: {}", userId, title);
        }
        return sent;
    }

    /**
     * 广播系统通知
     *
     * @param title   通知标题
     * @param content 通知内容
     */
    public void broadcast(String title, String content) {
        String message = buildMessage(title, content, "broadcast");
        handler.broadcast(message);
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
}

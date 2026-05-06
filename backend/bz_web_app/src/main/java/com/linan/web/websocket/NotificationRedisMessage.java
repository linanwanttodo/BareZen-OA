package com.linan.web.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 跨实例广播消息（Redis pub/sub 载体，3.3）
 *
 * <p>单实例内 onlineSessions 为内存 Map，多实例部署时用户可能连接在其他实例。
 * 本地推送失败（用户不在本实例）后，发布方将本消息写入 Redis channel
 * {@code ws:notify}，所有实例订阅后按 {@code instanceId} 去重：
 * 发布方实例忽略自己发出的消息（本地已尝试过），其他实例尝试本地推送。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRedisMessage {

    /** 发布方实例 ID（barezen.instance-id，空则启动时随机生成） */
    private String instanceId;

    /** 消息类型：user=定向推送 / broadcast=全员广播 */
    private String type;

    /** 目标用户 ID（type=user 时有效） */
    private Long userId;

    /** 推送的 JSON 载荷（标题/内容/时间） */
    private String message;
}
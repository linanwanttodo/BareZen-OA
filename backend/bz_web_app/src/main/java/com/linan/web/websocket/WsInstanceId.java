package com.linan.web.websocket;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 实例唯一标识（3.3 多实例 WebSocket 去重）
 *
 * <p>发布方（{@link NotificationService}）与订阅方（{@link NotificationRedisListener}）
 * 必须使用同一实例 ID 才能让"本实例忽略自己发布的消息"成立，故抽为单例组件：
 * 应用启动时解析一次 {@code barezen.instance-id}（未配置则随机生成 UUID）。</p>
 */
@Getter
@Component
public class WsInstanceId {

    private final String value;

    public WsInstanceId(@Value("${barezen.instance-id:}") String instanceId) {
        this.value = instanceId != null && !instanceId.isBlank()
                ? instanceId
                : UUID.randomUUID().toString();
    }
}
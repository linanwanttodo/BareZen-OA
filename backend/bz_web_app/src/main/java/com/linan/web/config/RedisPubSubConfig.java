package com.linan.web.config;

import com.linan.web.websocket.NotificationRedisListener;
import com.linan.web.websocket.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis pub/sub 配置（3.3 跨实例 WebSocket 通知）
 *
 * <p>注册 {@link RedisMessageListenerContainer} 并订阅 channel {@code ws:notify}，
 * 使各实例都能收到其他实例发布的跨实例通知广播（配合实例 ID 去重）。
 * 消息体为 JSON 字符串（StringRedisTemplate 发布），listener 直接实现
 * {@link org.springframework.data.redis.connection.MessageListener} 自行解码。</p>
 */
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationRedisListener notificationRedisListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationRedisListener,
                new ChannelTopic(NotificationService.WS_NOTIFY_CHANNEL));
        return container;
    }
}
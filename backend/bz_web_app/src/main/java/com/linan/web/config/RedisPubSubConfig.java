package com.linan.web.config;

import com.linan.web.websocket.NotificationRedisListener;
import com.linan.web.websocket.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.Executor;

/**
 * Redis pub/sub 配置（3.3 跨实例 WebSocket 通知）
 *
 * <p>注册 {@link RedisMessageListenerContainer} 并订阅 channel {@code ws:notify}，
 * 使各实例都能收到其他实例发布的跨实例通知广播（配合实例 ID 去重）。
 * 消息体为 JSON 字符串（StringRedisTemplate 发布），listener 直接实现
 * {@link org.springframework.data.redis.connection.MessageListener} 自行解码。</p>
 *
 * <p>容器显式绑定有界线程池与错误处理器：默认的 {@code SimpleAsyncTaskExecutor}
 * 会无界创建订阅线程；未设置 ErrorHandler 时，监听回调抛出的异常会被静默吞掉，
 * 导致跨实例通知丢失且无任何告警。此处统一记录并复用 async 线程池。</p>
 */
@Slf4j
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationRedisListener notificationRedisListener,
            @Qualifier("asyncTaskExecutor") Executor taskExecutor) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 复用有界异步线程池，避免订阅回调无界创建线程
        container.setTaskExecutor(taskExecutor);
        // 监听回调异常统一告警，防止跨实例通知静默丢失
        container.setErrorHandler(t -> log.error("Redis 订阅消息处理异常: channel={}",
                NotificationService.WS_NOTIFY_CHANNEL, t));
        container.addMessageListener(notificationRedisListener,
                new ChannelTopic(NotificationService.WS_NOTIFY_CHANNEL));
        return container;
    }
}
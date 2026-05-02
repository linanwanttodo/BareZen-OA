package com.linan.web.websocket;

import com.linan.core.common.event.NoticePublishEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 公告发布事件监听器
 *
 * <p>监听 {@link NoticePublishEvent}，在公告发布成功后通过 WebSocket
 * 向所有在线用户广播实时通知，实现"发布公告 → 在线用户实时收到提醒"。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 异步处理公告发布事件：广播实时通知，不阻塞公告发布请求
     */
    @Async
    @EventListener
    public void onNoticePublished(NoticePublishEvent event) {
        String title = event.getTitle() != null ? event.getTitle() : "新公告";
        notificationService.broadcast(title, event.getContent());
        log.info("公告已发布并广播通知: {}", title);
    }
}

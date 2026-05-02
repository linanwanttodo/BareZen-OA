package com.linan.web.websocket;

import com.linan.core.common.event.ApprovalCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审批完成事件监听器
 *
 * <p>监听 {@link ApprovalCompletedEvent}（请假/加班/出差/用车/用品申领审批结果），
 * 通过 WebSocket 向申请人定向推送实时通知。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class ApprovalNotificationEventListener {

    private final NotificationService notificationService;

    public ApprovalNotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 异步处理审批完成事件：向申请人推送通知，不阻塞审批请求
     */
    @Async
    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        if (event.getUserId() == null) {
            return;
        }
        String title = event.getTitle() != null ? event.getTitle() : "审批结果通知";
        notificationService.notifyUser(event.getUserId(), title, event.getContent());
        log.info("审批结果已推送用户 {}: {}", event.getUserId(), title);
    }
}

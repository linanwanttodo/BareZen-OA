package com.linan.web.websocket;

import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.common.event.NoticePublishEvent;
import com.linan.system.service.ISysWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Webhook 事件监听器
 *
 * <p>监听 {@link ApprovalCompletedEvent}（审批完成）与 {@link NoticePublishEvent}
 * （公告发布），异步推送 JSON 回调到订阅了对应事件的 Webhook 地址，
 * 实现「OA 事件 → 外部系统」实时联动（对应大型企业 OA 系统集成需求）。</p>
 *
 * <p>注意：发布方（业务 Service）已通过 {@code @Async} 或事件驱动异步化，
 * 此处监听本身不阻塞主流程；{@link ISysWebhookService#publishEvent} 内部亦为异步。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class WebhookEventListener {

    private final ISysWebhookService webhookService;

    public WebhookEventListener(ISysWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * 审批完成 → 推送 approval.completed 事件
     */
    @Async
    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", event.getUserId());
        payload.put("title", event.getTitle());
        payload.put("content", event.getContent());
        webhookService.publishEvent("approval.completed", "审批完成", payload);
        log.debug("审批完成事件已转推 Webhook: userId={}", event.getUserId());
    }

    /**
     * 公告发布 → 推送 notice.published 事件
     */
    @EventListener
    public void onNoticePublished(NoticePublishEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", event.getTitle());
        payload.put("content", event.getContent());
        payload.put("publishTime", event.getPublishTime() != null ? event.getPublishTime().toString() : null);
        webhookService.publishEvent("notice.published", "公告发布", payload);
        log.debug("公告发布事件已转推 Webhook: {}", event.getTitle());
    }
}

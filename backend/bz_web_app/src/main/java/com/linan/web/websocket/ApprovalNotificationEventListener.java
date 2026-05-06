package com.linan.web.websocket;

import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.mail.MailService;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.service.ISysMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 审批完成事件监听器
 *
 * <p>监听 {@link ApprovalCompletedEvent}（请假/加班/出差/用车/用品申领审批结果
 * 与新的待审批节点通知），按三条渠道分发：</p>
 * <ol>
 *   <li>WebSocket 在线推送（实时）</li>
 *   <li>站内信离线兜底：WS 推送失败（用户离线）时写入 {@code sys_message}，
 *       用户登录后通过消息中心查看</li>
 *   <li>邮件通知：SMTP 已配置（spring.mail.*）时异步发送到用户邮箱，未配置自动跳过</li>
 * </ol>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class ApprovalNotificationEventListener {

    private final NotificationService notificationService;
    private final ISysMessageService messageService;
    private final MailService mailService;
    private final SysUserMapper userMapper;

    public ApprovalNotificationEventListener(NotificationService notificationService,
                                             ISysMessageService messageService,
                                             MailService mailService,
                                             SysUserMapper userMapper) {
        this.notificationService = notificationService;
        this.messageService = messageService;
        this.mailService = mailService;
        this.userMapper = userMapper;
    }

    /**
     * 异步处理审批完成事件：WS 推送，离线时站内信 + 邮件兜底，不阻塞审批请求
     */
    @Async
    @EventListener
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        if (event.getUserId() == null) {
            return;
        }
        String title = event.getTitle() != null ? event.getTitle() : "审批结果通知";
        String content = event.getContent() != null ? event.getContent() : "";

        // 1. WS 在线推送；离线（返回 false）时进入站内信兜底
        boolean sent = notificationService.notifyUser(event.getUserId(), title, content);
        if (!sent) {
            messageService.saveMessage(event.getUserId(), title, content, "approval", null, null);
            log.info("用户 {} 离线，审批通知已落站内信: {}", event.getUserId(), title);
        }

        // 2. 邮件通知（SMTP 未配置时 MailService 内部静默跳过）
        sendMail(event.getUserId(), title, content);
        log.info("审批结果已处理用户 {}: {}", event.getUserId(), title);
    }

    private void sendMail(Long userId, String title, String content) {
        if (!mailService.isEnabled()) {
            return;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            log.debug("用户 {} 无邮箱，跳过邮件通知", userId);
            return;
        }
        mailService.sendMailAsync(user.getEmail(), title, content);
    }
}
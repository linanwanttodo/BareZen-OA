package com.linan.web.websocket;

import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.mail.MailService;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysUserMapper;
import com.linan.system.service.ISysMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批完成事件监听器单元测试（3.4 邮件可选渠道）
 *
 * <p>渠道语义：站内推送（WS + 站内信兜底）是默认渠道，始终执行；
 * 邮件是可选增强渠道，仅当事件 {@code mail=true}（发布方显式选择，
 * 如工资发放/放假通知）时才可能发送，普通审批通知默认不发邮件。</p>
 */
@ExtendWith(MockitoExtension.class)
class ApprovalNotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ISysMessageService messageService;

    @Mock
    private MailService mailService;

    @Mock
    private SysUserMapper userMapper;

    private ApprovalNotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ApprovalNotificationEventListener(
                notificationService, messageService, mailService, userMapper);
    }

    /** 默认事件：mail=false，仅站内推送 */
    private ApprovalCompletedEvent event(Long userId) {
        return new ApprovalCompletedEvent(userId, "请假审批已通过", "您的请假申请已通过");
    }

    /** 发布方显式选择邮件渠道的事件：mail=true */
    private ApprovalCompletedEvent mailRequestedEvent(Long userId) {
        return new ApprovalCompletedEvent(userId, "7月工资已发放", "您的7月工资已发放，请注意查收", true);
    }

    @Test
    @DisplayName("WS 在线（默认事件）：推送成功，不落站内信，不发邮件")
    void online_pushOnly_noMail() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);

        listener.onApprovalCompleted(event(10L));

        verify(messageService, never()).saveMessage(any(), anyString(), anyString(), anyString(), any(), any());
        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("WS 离线（默认事件）：落站内信兜底，不发邮件")
    void offline_saveMessage_noMail() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(false);

        listener.onApprovalCompleted(event(10L));

        verify(messageService).saveMessage(eq(10L), anyString(), anyString(), eq("approval"), any(), any());
        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("默认事件即使 SMTP 已配置且用户有邮箱：仍不发邮件（可选渠道默认关闭）")
    void defaultEvent_smtpReady_stillNoMail() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);

        listener.onApprovalCompleted(event(10L));

        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("mail=true 且 SMTP 已配置且用户有邮箱：发送邮件")
    void mailRequested_smtpReady_sendToUserEmail() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);
        when(mailService.isEnabled()).thenReturn(true);
        SysUser user = new SysUser();
        user.setEmail("user@barezen.com");
        when(userMapper.selectById(10L)).thenReturn(user);

        listener.onApprovalCompleted(mailRequestedEvent(10L));

        verify(mailService).sendMailAsync("user@barezen.com", "7月工资已发放", "您的7月工资已发放，请注意查收");
    }

    @Test
    @DisplayName("mail=true 但 SMTP 未配置：跳过邮件，站内推送不受影响")
    void mailRequested_smtpDisabled_skip() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);
        when(mailService.isEnabled()).thenReturn(false);

        listener.onApprovalCompleted(mailRequestedEvent(10L));

        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("mail=true 但用户无邮箱：跳过邮件")
    void mailRequested_noEmail_skip() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);
        when(mailService.isEnabled()).thenReturn(true);
        when(userMapper.selectById(10L)).thenReturn(new SysUser()); // email 为空

        listener.onApprovalCompleted(mailRequestedEvent(10L));

        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("userId 为空：直接忽略")
    void nullUserId_ignored() {
        listener.onApprovalCompleted(event(null));

        verify(notificationService, never()).notifyUser(any(), anyString(), anyString());
    }
}
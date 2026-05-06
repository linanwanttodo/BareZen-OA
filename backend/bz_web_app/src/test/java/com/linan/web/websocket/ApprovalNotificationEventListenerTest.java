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
 * 审批完成事件监听器单元测试（3.1 通知三渠道）
 *
 * <p>覆盖：WS 在线推送成功时不落站内信；用户离线时落站内信兜底；
 * 邮件仅在 SMTP 配置且用户有邮箱时发送。</p>
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

    private ApprovalCompletedEvent event(Long userId) {
        return new ApprovalCompletedEvent(userId, "请假审批已通过", "您的请假申请已通过");
    }

    @Test
    @DisplayName("WS 在线：推送成功，不落站内信")
    void online_pushOnly() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);

        listener.onApprovalCompleted(event(10L));

        verify(messageService, never()).saveMessage(any(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("WS 离线：推送失败，落站内信兜底")
    void offline_saveMessage() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(false);

        listener.onApprovalCompleted(event(10L));

        verify(messageService).saveMessage(eq(10L), anyString(), anyString(), eq("approval"), any(), any());
    }

    @Test
    @DisplayName("SMTP 未配置：不查用户、不发邮件")
    void mailDisabled_skipSend() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);
        when(mailService.isEnabled()).thenReturn(false);

        listener.onApprovalCompleted(event(10L));

        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("SMTP 已配置且用户有邮箱：发送邮件")
    void mailEnabled_sendToUserEmail() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);
        when(mailService.isEnabled()).thenReturn(true);
        SysUser user = new SysUser();
        user.setEmail("user@barezen.com");
        when(userMapper.selectById(10L)).thenReturn(user);

        listener.onApprovalCompleted(event(10L));

        verify(mailService).sendMailAsync("user@barezen.com", "请假审批已通过", "您的请假申请已通过");
    }

    @Test
    @DisplayName("SMTP 已配置但用户无邮箱：跳过邮件")
    void mailEnabled_noEmail_skip() {
        when(notificationService.notifyUser(eq(10L), anyString(), anyString())).thenReturn(true);
        when(mailService.isEnabled()).thenReturn(true);
        when(userMapper.selectById(10L)).thenReturn(new SysUser()); // email 为空

        listener.onApprovalCompleted(event(10L));

        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("userId 为空：直接忽略")
    void nullUserId_ignored() {
        listener.onApprovalCompleted(event(null));

        verify(notificationService, never()).notifyUser(any(), anyString(), anyString());
    }
}
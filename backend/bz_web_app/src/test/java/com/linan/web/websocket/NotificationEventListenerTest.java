package com.linan.web.websocket;

import com.linan.core.common.event.NoticePublishEvent;
import com.linan.core.mail.MailService;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公告发布事件监听器单元测试（3.4 公告邮件可选渠道）
 *
 * <p>渠道语义：站内 WebSocket 广播是默认渠道，始终执行；
 * 邮件是可选增强渠道，仅当事件 {@code mail=true}（发布方选择 sendEmail=Y）时
 * 向全员启用且有邮箱的用户发送，普通公告默认不发邮件。</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private MailService mailService;

    @Mock
    private SysUserMapper userMapper;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService, mailService, userMapper);
    }

    @Test
    @DisplayName("默认公告（mail=false）：站内广播，不发邮件，不查用户")
    void defaultNotice_broadcastOnly() {
        listener.onNoticePublished(new NoticePublishEvent("放假通知", "五一放假安排"));

        verify(notificationService).broadcast("放假通知", "五一放假安排");
        verify(userMapper, never()).selectList(any());
        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("mail=true 且 SMTP 已配置：站内广播 + 全员有邮箱用户逐个发邮件")
    void mailNotice_smtpReady_sendToAllWithEmail() {
        when(mailService.isEnabled()).thenReturn(true);
        SysUser u1 = new SysUser();
        u1.setEmail("a@barezen.com");
        SysUser u2 = new SysUser();
        u2.setEmail("b@barezen.com");
        when(userMapper.selectList(any())).thenReturn(List.of(u1, u2));

        listener.onNoticePublished(new NoticePublishEvent("7月工资已发放", "请查收", true));

        verify(notificationService).broadcast("7月工资已发放", "请查收");
        verify(mailService).sendMailAsync("a@barezen.com", "7月工资已发放", "请查收");
        verify(mailService).sendMailAsync("b@barezen.com", "7月工资已发放", "请查收");
    }

    @Test
    @DisplayName("mail=true 但 SMTP 未配置：站内广播正常，跳过邮件")
    void mailNotice_smtpDisabled_skip() {
        when(mailService.isEnabled()).thenReturn(false);

        listener.onNoticePublished(new NoticePublishEvent("放假通知", "五一放假安排", true));

        verify(notificationService).broadcast("放假通知", "五一放假安排");
        verify(userMapper, never()).selectList(any());
        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("mail=true 但无启用且有邮箱用户：跳过邮件")
    void mailNotice_noUsers_skip() {
        when(mailService.isEnabled()).thenReturn(true);
        when(userMapper.selectList(any())).thenReturn(List.of());

        listener.onNoticePublished(new NoticePublishEvent("放假通知", "五一放假安排", true));

        verify(notificationService).broadcast("放假通知", "五一放假安排");
        verify(mailService, never()).sendMailAsync(anyString(), anyString(), anyString());
    }
}
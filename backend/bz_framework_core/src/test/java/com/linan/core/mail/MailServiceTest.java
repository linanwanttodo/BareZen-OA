package com.linan.core.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邮件发送服务单元测试
 *
 * <p>覆盖：SMTP 未配置时静默跳过（isEnabled=false）、配置后异步发送、
 * 发送异常被吞掉不向上抛（邮件为辅助渠道不阻断主流程）。</p>
 */
class MailServiceTest {

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender mailSender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }

    @Test
    @DisplayName("SMTP 未配置（from 为空）：不可用且发送静默跳过")
    void sendMail_disabled_whenNoFrom() {
        MailService mailService = new MailService(provider(mock(JavaMailSender.class)), "");

        assertFalse(mailService.isEnabled());
        // 不抛异常
        mailService.sendMailAsync("user@barezen.com", "测试", "内容");
    }

    @Test
    @DisplayName("SMTP 已配置：发送 SimpleMailMessage，from/to/subject/content 透传")
    void sendMail_delivers() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailService mailService = new MailService(provider(mailSender), "noreply@barezen.com");

        assertTrue(mailService.isEnabled());
        mailService.sendMailAsync("a@b.com,c@d.com", "审批通知", "您的请假已通过");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("收件人为空：跳过发送")
    void sendMail_blankRecipient_skipped() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailService mailService = new MailService(provider(mailSender), "noreply@barezen.com");

        mailService.sendMailAsync("", "审批通知", "内容");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("发送异常：吞掉不抛出（不阻断主流程）")
    void sendMail_failure_swallowed() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        org.mockito.Mockito.doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        MailService mailService = new MailService(provider(mailSender), "noreply@barezen.com");

        // 不抛异常
        mailService.sendMailAsync("a@b.com", "审批通知", "内容");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("isEnabled 读取 spring.mail.username 配置")
    void enabled_readsConfiguredUsername() {
        MailService mailService = new MailService(provider(mock(JavaMailSender.class)), "noreply@barezen.com");

        assertTrue(mailService.isEnabled());
        // 验证 from 字段注入
        assertTrue(String.valueOf(ReflectionTestUtils.getField(mailService, "from")).startsWith("noreply"));
    }
}
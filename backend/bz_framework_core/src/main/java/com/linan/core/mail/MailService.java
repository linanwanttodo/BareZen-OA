package com.linan.core.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件发送服务
 *
 * <p>基于 {@code spring.mail.*}（Spring Boot 自动装配 {@link JavaMailSender}）。
 * SMTP 未配置时（{@code spring.mail.host} 为空）JavaMailSender 不会创建，
 * 通过 {@link ObjectProvider} 获取空对象，sendMail 静默跳过，不抛异常，
 * 保证无邮件环境（如本地开发）下业务不受影响。</p>
 */
@Slf4j
@Service
public class MailService {

    /** JavaMailSender（SMTP 未配置时为 null） */
    private final JavaMailSender mailSender;

    /** 发送方地址（spring.mail.username，未配置时为空） */
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.from = from;
    }

    /**
     * 邮件是否可用（SMTP 已配置）
     */
    public boolean isEnabled() {
        return mailSender != null && StringUtils.hasText(from);
    }

    /**
     * 异步发送邮件：SMTP 未配置或发送失败均不抛出，由日志记录
     *
     * @param to      收件人（多个用逗号分隔）
     * @param subject 主题
     * @param content 纯文本内容
     */
    @Async
    public void sendMailAsync(String to, String subject, String content) {
        if (!isEnabled()) {
            log.debug("SMTP 未配置，跳过邮件发送: to={}, subject={}", to, subject);
            return;
        }
        if (!StringUtils.hasText(to)) {
            log.warn("收件人为空，跳过邮件发送: subject={}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to.split(","));
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            // 邮件为辅助通知渠道，失败不阻断主流程
            log.warn("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }
}
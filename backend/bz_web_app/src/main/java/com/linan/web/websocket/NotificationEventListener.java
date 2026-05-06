package com.linan.web.websocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.event.NoticePublishEvent;
import com.linan.core.mail.MailService;
import com.linan.system.entity.SysUser;
import com.linan.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 公告发布事件监听器
 *
 * <p>监听 {@link NoticePublishEvent}，在公告发布成功后按渠道分发：</p>
 * <ol>
 *   <li>WebSocket 向所有在线用户广播实时通知（站内默认渠道，始终执行）</li>
 *   <li>邮件为<strong>可选增强渠道</strong>：仅当事件 {@code mail=true}
 *       （发布方在公告上选择 sendEmail=Y，如工资发放、放假通知等需要邮箱留痕的场景）时，
 *       向所有启用且有邮箱的用户异步发送，普通公告默认不发邮件</li>
 * </ol>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final MailService mailService;
    private final SysUserMapper userMapper;

    public NotificationEventListener(NotificationService notificationService,
                                     MailService mailService,
                                     SysUserMapper userMapper) {
        this.notificationService = notificationService;
        this.mailService = mailService;
        this.userMapper = userMapper;
    }

    /**
     * 异步处理公告发布事件：站内广播实时通知；仅当 mail=true 时向全员有邮箱用户发邮件，不阻塞发布请求
     */
    @Async
    @EventListener
    public void onNoticePublished(NoticePublishEvent event) {
        String title = event.getTitle() != null ? event.getTitle() : "新公告";
        notificationService.broadcast(title, event.getContent());

        if (event.isMail()) {
            sendMailToAll(title, event.getContent());
        }
        log.info("公告已发布并广播通知: {}", title);
    }

    /**
     * 全员公告邮件：SMTP 未配置或无收件人时静默跳过（邮件为辅助渠道，不影响站内推送）
     */
    private void sendMailToAll(String title, String content) {
        if (!mailService.isEnabled()) {
            log.debug("SMTP 未配置，跳过公告邮件: title={}", title);
            return;
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, "0")
                .isNotNull(SysUser::getEmail)
                .ne(SysUser::getEmail, ""));
        if (users.isEmpty()) {
            log.debug("无启用且有邮箱的用户，跳过公告邮件: title={}", title);
            return;
        }
        for (SysUser user : users) {
            mailService.sendMailAsync(user.getEmail(), title, content);
        }
        log.info("公告邮件已投递 {} 人: {}", users.size(), title);
    }
}
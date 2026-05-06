package com.linan.core.common.event;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批完成事件
 *
 * <p>审批（请假/加班/出差/用车/用品申领）通过或驳回后发布，
 * 由 bz_web_app 层的监听器转换为 WebSocket 通知推送给申请人。</p>
 *
 * <p><strong>通知渠道语义</strong>：站内推送（WebSocket 在线 + 站内信离线兜底）
 * 是默认渠道；邮件是<strong>可选增强渠道</strong>——仅当 {@link #mail}=true
 * 时才同步发送邮件（如工资发放、放假通知等需要邮箱留痕的场景），
 * 普通审批通知默认不发邮件。</p>
 *
 * @author BareZen Team
 */
@Data
@NoArgsConstructor
public class ApprovalCompletedEvent {

    /** 被通知用户ID（申请人） */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否同时发送邮件（可选渠道，默认 false=仅站内推送） */
    private boolean mail;

    public ApprovalCompletedEvent(Long userId, String title, String content) {
        this(userId, title, content, false);
    }

    public ApprovalCompletedEvent(Long userId, String title, String content, boolean mail) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.mail = mail;
    }
}
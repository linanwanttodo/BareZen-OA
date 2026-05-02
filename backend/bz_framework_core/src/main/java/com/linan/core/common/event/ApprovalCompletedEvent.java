package com.linan.core.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批完成事件
 *
 * <p>审批（请假/加班/出差/用车/用品申领）通过或驳回后发布，
 * 由 bz_web_app 层的监听器转换为 WebSocket 通知推送给申请人。</p>
 *
 * @author BareZen Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCompletedEvent {

    /** 被通知用户ID（申请人） */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;
}

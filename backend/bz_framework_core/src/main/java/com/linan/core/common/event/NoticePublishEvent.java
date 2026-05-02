package com.linan.core.common.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 公告发布事件
 *
 * <p>由 {@code SysNoticeServiceImpl.publish()} 在公告发布成功后发布，
 * Web 模块的 {@code NotificationEventListener} 监听该事件并推送 WebSocket 广播，
 * 实现"发布公告 → 在线用户实时收到通知"的完整链路。</p>
 *
 * @author BareZen Team
 */
@Getter
public class NoticePublishEvent {

    /** 公告标题 */
    private final String title;

    /** 公告内容 */
    private final String content;

    /** 发布时间 */
    private final LocalDateTime publishTime;

    public NoticePublishEvent(String title, String content) {
        this.title = title;
        this.content = content;
        this.publishTime = LocalDateTime.now();
    }
}

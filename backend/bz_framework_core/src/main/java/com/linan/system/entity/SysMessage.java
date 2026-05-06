package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内消息实体，对应 sys_message（WebSocket 离线兜底）
 *
 * <p>审批完成 / 新的待办等事件在 WS 推送失败（用户离线）时落库，
 * 用户登录后通过消息中心查看与标记已读。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    /** 收件人用户ID */
    private Long userId;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型（approval=审批通知/system=系统消息/notice=公告） */
    private String msgType;

    /** 关联业务类型（leave/overtime/travel/vehicle/supply） */
    private String bizType;

    /** 关联业务单ID */
    private Long bizId;

    /** 是否已读（0=未读 1=已读） */
    private Integer isRead;

    /** 阅读时间 */
    private java.time.LocalDateTime readTime;

    /** 已读标记（非持久化，接口返回用，冗余 isRead 便于前端布尔语义） */
    @TableField(exist = false)
    private Boolean read;
}
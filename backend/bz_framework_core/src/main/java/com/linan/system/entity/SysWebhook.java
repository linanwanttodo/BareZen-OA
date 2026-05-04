package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Webhook 事件订阅实体，对应 sys_webhook 表
 *
 * <p>事件（审批完成/公告发布等）发生时向配置的 URL 推送 JSON 回调，
 * 实现「OA 事件 → 外部系统」的实时联动。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_webhook")
public class SysWebhook extends BaseEntity {

    /** 订阅名称 */
    private String name;

    /** 回调地址（HTTP/HTTPS） */
    private String url;

    /** 签名密钥（可选，用于 HMAC-SHA256 签名校验） */
    private String secret;

    /** 订阅事件，逗号分隔：approval.completed / notice.published */
    private String events;

    /** 状态：0=启用 1=禁用 */
    private String status;
}

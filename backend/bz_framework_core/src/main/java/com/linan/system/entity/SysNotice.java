package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 通知公告实体，对应 sys_notice */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {
    private String noticeTitle;
    private String noticeContent;
    private String noticeType;
    private String status;
    private String scope;
    /** 是否强制阅读（Y=是 N=否） */
    private String requireRead;
    /** 发布时是否同步发送邮件（Y=是 N=否，默认 N） */
    private String sendEmail;
    /** 当前用户是否已读（非持久化，接口返回用） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Boolean isRead;
}
package com.linan.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作日志实体，对应 hr_work_log 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_work_log")
public class HrWorkLog extends BaseEntity {

    /** 作者用户ID */
    private Long userId;

    /** 作者姓名（冗余） */
    private String userName;

    /** 作者部门（冗余） */
    private String deptName;

    /** 日志类型：daily=日报 weekly=周报 */
    @NotBlank(message = "日志类型不能为空")
    private String logType;

    /** 日志日期（yyyy-MM-dd，周报填周结束日） */
    private String logDate;

    /** 日志内容 */
    @NotBlank(message = "日志内容不能为空")
    private String content;

    /** 批阅人用户ID */
    private Long reviewerId;

    /** 批阅人姓名 */
    private String reviewerName;

    /** 批阅时间 */
    private String reviewTime;

    /** 批阅意见 */
    private String reviewRemark;
}

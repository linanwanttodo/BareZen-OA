package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志实体，对应 sys_job_log 表
 *
 * 记录每次任务调用的执行情况，包括开始时间、结束时间、执行时长和状态。
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_log")
public class SysJobLog extends BaseEntity {

    /** 任务 ID */
    private Long jobId;

    /** 任务名称 */
    private String jobName;

    /** 任务组名 */
    private String jobGroup;

    /** 调用目标字符串（beanName.methodName） */
    private String invokeTarget;

    /** cron 执行表达式 */
    private String cronExpression;

    /** 执行状态：0=成功，1=失败 */
    private String status;

    /** 异常信息（失败时记录堆栈） */
    private String errorMsg;

    /** 开始执行时间 */
    private LocalDateTime startTime;

    /** 停止执行时间 */
    private LocalDateTime stopTime;

    /** 执行时长（毫秒） */
    private Long duration;
}

package com.linan.core.spi.audit;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志模型（core 层传输对象）
 *
 * <p>由 {@link com.linan.core.quartz.ScheduleRunnable} 采集，经
 * {@link JobLogSink} 交由系统模块适配落库到 sys_job_log。</p>
 *
 * @author BareZen Team
 */
@Data
public class JobExecutionLog {

    /** 任务 ID */
    private Long jobId;

    /** 任务名称 */
    private String jobName;

    /** 任务组 */
    private String jobGroup;

    /** 调用目标 beanName.methodName */
    private String invokeTarget;

    /** cron 表达式 */
    private String cronExpression;

    /** 状态：0=成功 1=失败 */
    private String status;

    /** 错误信息 */
    private String errorMsg;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime stopTime;

    /** 执行时长（毫秒） */
    private Long duration;
}
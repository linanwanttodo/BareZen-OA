package com.linan.core.spi.audit;

/**
 * 定时任务日志持久化 SPI（由系统模块实现）
 *
 * <p>反转 core → system 依赖方向：Quartz 执行器只写本契约，
 * 落库到 sys_job_log 的映射由 system 侧适配器完成。</p>
 *
 * @author BareZen Team
 */
public interface JobLogSink {

    /** 持久化一条任务执行日志 */
    void save(JobExecutionLog log);
}
package com.linan.core.spi.audit;

import java.time.LocalDateTime;

/**
 * 操作日志持久化 SPI（由系统模块注入实现）
 *
 * <p>core 的 AOP 切面与清理任务只依赖本契约的语义，
 * 具体写入 sys_oper_log 及 30 天清理由 system 侧适配器执行。</p>
 *
 * @author BareZen Team
 */
public interface OperLogSink {

    /** 持久化一条操作日志 */
    void save(OperationLog log);

    /** 统计创建时间早于截止时刻的日志条数 */
    long countBefore(LocalDateTime deadline);

    /** 删除创建时间早于截止时刻的日志，返回是否删除了记录 */
    boolean removeBefore(LocalDateTime deadline);
}
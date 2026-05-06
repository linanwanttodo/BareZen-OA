package com.linan.core.quartz;

import com.linan.core.spi.audit.OperLogSink;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 系统清理任务
 *
 * 作为定时任务 bean 被 Quartz 调度，通过 ScheduleRunnable 反射调用。
 * bean 名称为 "cleanTask"，对应调度配置中的 invokeTarget=cleanTask.cleanOperLog。
 *
 * @author BareZen Team
 */
@Slf4j
@Component("cleanTask")
@RequiredArgsConstructor
public class CleanTask {

    private final OperLogSink operLogSink;

    /**
     * 清理操作日志
     *
     * 删除 30 天前的操作日志记录，避免日志表无限膨胀。
     */
    public void cleanOperLog() {
        try {
            LocalDateTime deadline = LocalDateTime.now().minusDays(30);

            long count = operLogSink.countBefore(deadline);

            if (count > 0) {
                operLogSink.removeBefore(deadline);
                log.info("系统清理任务：已清理 {} 条操作日志（30 天前）", count);
            } else {
                log.info("系统清理任务：无 30 天前操作日志需要清理");
            }
        } catch (Exception e) {
            log.error("系统清理任务执行异常", e);
        }
    }
}

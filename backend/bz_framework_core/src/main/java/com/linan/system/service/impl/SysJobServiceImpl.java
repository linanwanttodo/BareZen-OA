package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.SysJob;
import com.linan.system.mapper.SysJobLogMapper;
import com.linan.system.mapper.SysJobMapper;
import com.linan.system.service.ISysJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 定时任务服务实现
 *
 * 整合 Quartz Scheduler，在任务新增/更新/删除时同步管理对应的 Quartz 触发器。
 *
 * @author BareZen Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJob> implements ISysJobService {

    private final SysJobMapper jobMapper;

    private final SysJobLogMapper jobLogMapper;

    private final Scheduler scheduler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addJob(SysJob job) {
        if (!checkCronExpression(job.getCronExpression())) {
            throw new IllegalArgumentException("cron 表达式不合法: " + job.getCronExpression());
        }
        // 保存任务到数据库
        jobMapper.insert(job);

        // 创建 Quartz 触发器（状态 0=暂停 1=正常：显式暂停的任务不得进入调度，
        // 否则 sys_job 显示暂停而实际在跑，与 pauseJob/resumeJob 的语义一致）
        if ("0".equals(job.getStatus())) {
            log.info("定时任务 [{}] 以暂停状态创建，不注册调度", job.getJobName());
            return;
        }
        createScheduleJob(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(SysJob job) {
        if (!checkCronExpression(job.getCronExpression())) {
            throw new IllegalArgumentException("cron 表达式不合法: " + job.getCronExpression());
        }
        // 更新数据库中的任务（0 行受影响说明任务已不存在，必须显式失败而非静默继续调度）
        int rows = jobMapper.updateById(job);
        if (rows == 0) {
            throw new BusinessException("任务不存在或已删除");
        }

        // 重新调度 Quartz 触发器
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString(), jobGroup);
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                    .build();

            // Quartz 语义：返回 null 表示触发器不存在、未能重新调度
            java.util.Date nextFireTime = scheduler.rescheduleJob(triggerKey, trigger);
            if (nextFireTime == null) {
                // 降级路径：触发器不存在（重启丢失调度 / 暂停后被清理 /
                // 以暂停状态创建未注册），按最新配置重建；
                // 暂停状态（0）的任务不重建调度，维持"未调度"与状态一致
                log.warn("定时任务 [{}] 触发器不存在，降级为删除后重建调度", job.getJobName());
                scheduler.deleteJob(JobKey.jobKey(jobId.toString(), jobGroup));
                if (!"0".equals(job.getStatus())) {
                    createScheduleJob(job);
                }
            }
            log.info("定时任务 [{}] 已更新触发器: {}", job.getJobName(), job.getCronExpression());
        } catch (SchedulerException e) {
            log.error("更新定时任务触发器失败, jobId: {}", jobId, e);
            throw new BusinessException("更新定时任务触发器失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(SysJob job) {
        // 从数据库中删除
        jobMapper.deleteById(job.getId());

        // 移除 Quartz 触发器
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString(), jobGroup);
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(JobKey.jobKey(jobId.toString(), jobGroup));
            log.info("定时任务 [{}] 已删除", job.getJobName());
        } catch (SchedulerException e) {
            log.error("删除定时任务触发器失败, jobId: {}", jobId, e);
            throw new BusinessException("删除定时任务触发器失败");
        }
    }

    @Override
    public void pauseJob(SysJob job) {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            scheduler.pauseJob(JobKey.jobKey(jobId.toString(), jobGroup));
            // 状态（0=暂停 1=正常）：暂停任务置为 0，避免 UI 开关状态与实际调度状态颠倒
            job.setStatus("0");
            jobMapper.updateById(job);
            log.info("定时任务 [{}] 已暂停", job.getJobName());
        } catch (SchedulerException e) {
            log.error("暂停定时任务失败, jobId: {}", jobId, e);
            throw new BusinessException("暂停定时任务失败");
        }
    }

    @Override
    public void resumeJob(SysJob job) {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            scheduler.resumeJob(JobKey.jobKey(jobId.toString(), jobGroup));
            // 触发器缺失防护：resumeJob 只能唤醒已存在的暂停触发器；若调度在
            // 重启后丢失（内存 JobStore）或任务以暂停状态创建，则按配置重建调度，
            // 否则状态置为正常但实际不会被触发（假恢复）
            Trigger existing = scheduler.getTrigger(
                    TriggerKey.triggerKey(jobId.toString(), jobGroup));
            if (existing == null) {
                log.warn("定时任务 [{}] 恢复时触发器不存在，按配置重建调度", job.getJobName());
                createScheduleJob(job);
            }
            // 状态（0=暂停 1=正常）：恢复任务置为 1，避免 UI 开关状态与实际调度状态颠倒
            job.setStatus("1");
            jobMapper.updateById(job);
            log.info("定时任务 [{}] 已恢复", job.getJobName());
        } catch (SchedulerException e) {
            log.error("恢复定时任务失败, jobId: {}", jobId, e);
            throw new BusinessException("恢复定时任务失败");
        }
    }

    @Override
    public void runJob(SysJob job) {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            scheduler.triggerJob(JobKey.jobKey(jobId.toString(), jobGroup));
            log.info("定时任务 [{}] 已手动触发执行", job.getJobName());
        } catch (SchedulerException e) {
            log.error("手动触发定时任务失败, jobId: {}", jobId, e);
            throw new BusinessException("手动触发定时任务失败");
        }
    }

    @Override
    public boolean checkCronExpression(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }

    /**
     * 应用就绪后恢复调度（启动补建）
     *
     * <p>Quartz 使用内存 JobStore（默认），进程重启后全部触发器丢失，而 sys_job 表中
     * status='1'（正常）的任务按业务语义应处于可触发状态。此处扫描调度缺失的启用任务
     * 并按配置重建 JobDetail + CronTrigger，消除"状态正常但实际永不执行"的孤儿任务。
     * cron 非法等个别任务失败仅告警跳过，不阻断应用启动。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void restoreSchedulesOnStartup() {
        List<SysJob> enabledJobs;
        try {
            enabledJobs = jobMapper.selectList(new LambdaQueryWrapper<SysJob>()
                    .eq(SysJob::getStatus, "1"));
        } catch (Exception e) {
            log.error("定时任务启动恢复：查询启用任务失败，跳过恢复", e);
            return;
        }
        int restored = 0;
        for (SysJob job : enabledJobs) {
            try {
                TriggerKey triggerKey = TriggerKey.triggerKey(
                        job.getId().toString(), job.getJobGroup());
                if (scheduler.getTrigger(triggerKey) != null) {
                    continue;
                }
                createScheduleJob(job);
                restored++;
            } catch (Exception e) {
                log.error("定时任务启动恢复失败, jobId={}, jobName={}",
                        job.getId(), job.getJobName(), e);
            }
        }
        if (restored > 0) {
            log.info("定时任务启动恢复完成：补建调度 {} 个（启用任务共 {} 个）",
                    restored, enabledJobs.size());
        }
    }

    // ============ 内部辅助方法 ============

    /**
     * 创建 Quartz 调度任务
     *
     * 为新增的任务创建 JobDetail 和 CronTrigger，并注册到 Scheduler。
     */
    private void createScheduleJob(SysJob job) {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            // 构建 JobDetail
            JobDetail jobDetail = JobBuilder.newJob(com.linan.core.quartz.ScheduleRunnable.class)
                    .withIdentity(jobId.toString(), jobGroup)
                    .usingJobData("invokeTarget", job.getInvokeTarget())
                    .withDescription(job.getJobName())
                    .build();

            // 构建 CronTrigger
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobId.toString(), jobGroup)
                    .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("定时任务 [{}] 已创建调度: {}", job.getJobName(), job.getCronExpression());
        } catch (SchedulerException e) {
            log.error("创建定时任务调度失败, jobId: {}", jobId, e);
            throw new BusinessException("创建定时任务调度失败");
        }
    }
}

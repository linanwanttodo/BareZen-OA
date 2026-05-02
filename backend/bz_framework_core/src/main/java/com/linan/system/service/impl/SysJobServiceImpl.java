package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysJob;
import com.linan.system.mapper.SysJobLogMapper;
import com.linan.system.mapper.SysJobMapper;
import com.linan.system.service.ISysJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定时任务服务实现
 *
 * 整合 Quartz Scheduler，在任务新增/更新/删除时同步管理对应的 Quartz 触发器。
 *
 * @author BareZen Team
 */
@Slf4j
@Service
public class SysJobServiceImpl extends ServiceImpl<SysJobMapper, SysJob> implements ISysJobService {

    @Autowired
    private SysJobMapper jobMapper;

    @Autowired
    private SysJobLogMapper jobLogMapper;

    @Autowired
    private Scheduler scheduler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addJob(SysJob job) {
        if (!checkCronExpression(job.getCronExpression())) {
            throw new IllegalArgumentException("cron 表达式不合法: " + job.getCronExpression());
        }
        // 保存任务到数据库
        jobMapper.insert(job);

        // 创建 Quartz 触发器
        createScheduleJob(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(SysJob job) {
        if (!checkCronExpression(job.getCronExpression())) {
            throw new IllegalArgumentException("cron 表达式不合法: " + job.getCronExpression());
        }
        // 更新数据库中的任务
        jobMapper.updateById(job);

        // 重新调度 Quartz 触发器
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString(), jobGroup);
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                    .build();

            scheduler.rescheduleJob(triggerKey, trigger);
            log.info("定时任务 [{}] 已更新触发器: {}", job.getJobName(), job.getCronExpression());
        } catch (SchedulerException e) {
            log.error("更新定时任务触发器失败, jobId: {}", jobId, e);
            throw new RuntimeException("更新定时任务触发器失败", e);
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
            throw new RuntimeException("删除定时任务触发器失败", e);
        }
    }

    @Override
    public void pauseJob(SysJob job) {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            scheduler.pauseJob(JobKey.jobKey(jobId.toString(), jobGroup));
            job.setStatus("1");
            jobMapper.updateById(job);
            log.info("定时任务 [{}] 已暂停", job.getJobName());
        } catch (SchedulerException e) {
            log.error("暂停定时任务失败, jobId: {}", jobId, e);
            throw new RuntimeException("暂停定时任务失败", e);
        }
    }

    @Override
    public void resumeJob(SysJob job) {
        Long jobId = job.getId();
        String jobGroup = job.getJobGroup();

        try {
            scheduler.resumeJob(JobKey.jobKey(jobId.toString(), jobGroup));
            job.setStatus("0");
            jobMapper.updateById(job);
            log.info("定时任务 [{}] 已恢复", job.getJobName());
        } catch (SchedulerException e) {
            log.error("恢复定时任务失败, jobId: {}", jobId, e);
            throw new RuntimeException("恢复定时任务失败", e);
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
            throw new RuntimeException("手动触发定时任务失败", e);
        }
    }

    @Override
    public boolean checkCronExpression(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
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
            throw new RuntimeException("创建定时任务调度失败", e);
        }
    }
}

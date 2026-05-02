package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysJob;

/**
 * 定时任务服务接口
 *
 * 提供任务管理的业务方法，包括 CRUD、状态变更和 cron 校验。
 * 内部整合 Quartz Scheduler 进行任务调度。
 *
 * @author BareZen Team
 */
public interface ISysJobService extends IService<SysJob> {

    /**
     * 新增定时任务
     *
     * @param job 任务实体
     */
    void addJob(SysJob job);

    /**
     * 更新定时任务（同时更新 Quartz 触发器）
     *
     * @param job 任务实体
     */
    void updateJob(SysJob job);

    /**
     * 删除定时任务（同时移除 Quartz 触发器）
     *
     * @param job 任务实体
     */
    void deleteJob(SysJob job);

    /**
     * 暂停定时任务
     *
     * @param job 任务实体
     */
    void pauseJob(SysJob job);

    /**
     * 恢复被暂停的定时任务
     *
     * @param job 任务实体
     */
    void resumeJob(SysJob job);

    /**
     * 立即执行一次定时任务
     *
     * @param job 任务实体
     */
    void runJob(SysJob job);

    /**
     * 校验 cron 表达式是否合法
     *
     * @param cronExpression cron 表达式
     * @return true 表示合法，false 表示非法
     */
    boolean checkCronExpression(String cronExpression);
}

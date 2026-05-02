package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysJob;
import com.linan.system.service.ISysJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 定时任务控制器
 *
 * 提供定时任务的管理接口，包括 CRUD、状态变更和手动触发。
 *
 * @author BareZen Team
 */
@Slf4j
@Tag(name = "定时任务", description = "系统监控 - 定时任务管理")
@RestController
@RequestMapping("/api/system/v1/jobs")
public class SysJobController {

    @Autowired
    private ISysJobService jobService;

    // ============ 分页查询 ============

    /**
     * 分页查询定时任务列表
     *
     * @param pageNum  当前页码（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @param jobName  任务名称（可选，模糊查询）
     * @param status   任务状态（可选）
     * @return 分页结果
     */
    @RequirePermission("monitor:job:list")
    @Operation(summary = "分页查询定时任务")
    @GetMapping("/page")
    public Result<PageResult<SysJob>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String status) {
        Page<SysJob> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysJob> wrapper = new LambdaQueryWrapper<SysJob>()
                .like(jobName != null && !jobName.isEmpty(), SysJob::getJobName, jobName)
                .eq(status != null && !status.isEmpty(), SysJob::getStatus, status)
                .orderByAsc(SysJob::getCreateTime);
        IPage<SysJob> result = jobService.page(page, wrapper);
        return Result.page(result);
    }

    // ============ 查询单个 ============

    /**
     * 根据 ID 获取定时任务详情
     *
     * @param id 任务 ID
     * @return 任务实体
     */
    @RequirePermission("monitor:job:list")
    @Operation(summary = "查询定时任务详情")
    @GetMapping("/{id}")
    public Result<SysJob> getById(@PathVariable Long id) {
        SysJob job = jobService.getById(id);
        return job != null ? Result.success(job) : Result.error("任务不存在");
    }

    // ============ 新增 ============

    /**
     * 新增定时任务
     *
     * @param job 任务实体（JSON 格式）
     * @return 操作结果
     */
    @RequirePermission("monitor:job:add")
    @OperLog(title = "定时任务", businessType = 1)
    @Operation(summary = "新增定时任务")
    @PostMapping
    public Result<Void> add(@RequestBody SysJob job) {
        try {
            jobService.addJob(job);
            log.info("新增定时任务: {}", job.getJobName());
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            log.warn("新增定时任务参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("新增定时任务失败", e);
            return Result.error("新增定时任务失败，请稍后重试");
        }
    }

    // ============ 修改 ============

    /**
     * 修改定时任务
     *
     * @param job 任务实体（JSON 格式，需包含 id）
     * @return 操作结果
     */
    @RequirePermission("monitor:job:edit")
    @OperLog(title = "定时任务", businessType = 2)
    @Operation(summary = "修改定时任务")
    @PutMapping
    public Result<Void> update(@RequestBody SysJob job) {
        if (job.getId() == null) {
            return Result.error("任务 ID 不能为空");
        }
        try {
            jobService.updateJob(job);
            log.info("修改定时任务: id={}, name={}", job.getId(), job.getJobName());
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            log.warn("修改定时任务参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("修改定时任务失败", e);
            return Result.error("修改定时任务失败，请稍后重试");
        }
    }

    // ============ 删除 ============

    /**
     * 删除定时任务
     *
     * @param id 任务 ID
     * @return 操作结果
     */
    @RequirePermission("monitor:job:delete")
    @OperLog(title = "定时任务", businessType = 3)
    @Operation(summary = "删除定时任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysJob job = jobService.getById(id);
        if (job == null) {
            return Result.error("任务不存在");
        }
        try {
            jobService.deleteJob(job);
            log.info("删除定时任务: id={}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除定时任务失败", e);
            return Result.error("删除定时任务失败，请稍后重试");
        }
    }

    // ============ 暂停 ============

    /**
     * 暂停定时任务
     *
     * @param id 任务 ID
     * @return 操作结果
     */
    @RequirePermission("monitor:job:edit")
    @OperLog(title = "定时任务", businessType = 2)
    @Operation(summary = "暂停定时任务")
    @PutMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id) {
        SysJob job = jobService.getById(id);
        if (job == null) {
            return Result.error("任务不存在");
        }
        try {
            jobService.pauseJob(job);
            log.info("暂停定时任务: id={}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("暂停定时任务失败", e);
            return Result.error("暂停定时任务失败，请稍后重试");
        }
    }

    // ============ 恢复 ============

    /**
     * 恢复被暂停的定时任务
     *
     * @param id 任务 ID
     * @return 操作结果
     */
    @RequirePermission("monitor:job:edit")
    @OperLog(title = "定时任务", businessType = 2)
    @Operation(summary = "恢复定时任务")
    @PutMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id) {
        SysJob job = jobService.getById(id);
        if (job == null) {
            return Result.error("任务不存在");
        }
        try {
            jobService.resumeJob(job);
            log.info("恢复定时任务: id={}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("恢复定时任务失败", e);
            return Result.error("恢复定时任务失败，请稍后重试");
        }
    }

    // ============ 立即执行 ============

    /**
     * 立即执行一次定时任务（不影响原有调度计划）
     *
     * @param id 任务 ID
     * @return 操作结果
     */
    @RequirePermission("monitor:job:edit")
    @OperLog(title = "定时任务", businessType = 2)
    @Operation(summary = "立即执行定时任务")
    @PutMapping("/{id}/run")
    public Result<Void> run(@PathVariable Long id) {
        SysJob job = jobService.getById(id);
        if (job == null) {
            return Result.error("任务不存在");
        }
        try {
            jobService.runJob(job);
            log.info("手动触发定时任务: id={}", id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("手动触发定时任务失败", e);
            return Result.error("手动触发定时任务失败，请稍后重试");
        }
    }

    // ============ 校验 cron ============

    /**
     * 校验 cron 表达式是否合法
     *
     * @param cronExpression cron 表达式
     * @return 校验结果
     */
    @RequirePermission("monitor:job:list")
    @Operation(summary = "校验 Cron 表达式")
    @GetMapping("/check-cron")
    public Result<Boolean> checkCron(@RequestParam String cronExpression) {
        boolean valid = jobService.checkCronExpression(cronExpression);
        return Result.success(valid);
    }
}

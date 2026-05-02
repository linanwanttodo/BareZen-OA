package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.service.IHrScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 个人日程 Controller（用户前台 /api/app/v1/schedules）
 *
 * <p>员工自助管理个人日程：新增、查询、更新（含完成/取消状态变更）、删除。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "个人日程", description = "用户前台 - 日程管理")
@RestController
@RequestMapping("/api/app/v1/schedules")
public class AppScheduleController {

    private static final Logger log = LoggerFactory.getLogger(AppScheduleController.class);

    @Autowired
    private IHrScheduleService scheduleService;

    /**
     * 我的日程列表
     * GET /api/app/v1/schedules
     */
    @Operation(summary = "我的日程列表")
    @GetMapping
    public Result<PageResult<HrSchedule>> mySchedules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            LambdaQueryWrapper<HrSchedule> wrapper = new LambdaQueryWrapper<HrSchedule>()
                    .eq(HrSchedule::getUserId, userId);
            if (status != null && !status.isEmpty()) {
                wrapper.eq(HrSchedule::getStatus, status);
            }
            if (date != null && !date.isEmpty()) {
                wrapper.likeRight(HrSchedule::getStartTime, date);
            }
            wrapper.orderByAsc(HrSchedule::getStartTime);
            IPage<HrSchedule> result = scheduleService.page(new Page<>(page, pageSize), wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询我的日程失败, userId={}", userId, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 新增日程
     * POST /api/app/v1/schedules
     */
    @Operation(summary = "新增日程")
    @PostMapping
    public Result<Void> addSchedule(@RequestBody HrSchedule schedule) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (schedule.getTitle() == null || schedule.getTitle().isBlank()) {
            return Result.error("日程标题不能为空");
        }
        if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return Result.error("请选择日程时间");
        }
        if (schedule.getStartTime().compareTo(schedule.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        try {
            schedule.setUserId(userId);
            if (schedule.getStatus() == null || schedule.getStatus().isEmpty()) {
                schedule.setStatus("0");
            }
            boolean ok = scheduleService.save(schedule);
            return ok ? Result.success(null) : Result.error("新增失败");
        } catch (Exception e) {
            log.error("新增日程失败, userId={}", userId, e);
            return Result.error("新增失败");
        }
    }

    /**
     * 更新日程（含状态变更：完成/取消）
     * PUT /api/app/v1/schedules
     */
    @Operation(summary = "更新日程")
    @PutMapping
    public Result<Void> updateSchedule(@RequestBody HrSchedule schedule) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        HrSchedule existing = scheduleService.getById(schedule.getId());
        if (existing == null) {
            return Result.error("日程不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            return Result.error("只能操作自己的日程");
        }
        try {
            boolean ok = scheduleService.updateById(schedule);
            return ok ? Result.success(null) : Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新日程失败, userId={}", userId, e);
            return Result.error("更新失败");
        }
    }

    /**
     * 删除日程
     * DELETE /api/app/v1/schedules/{id}
     */
    @Operation(summary = "删除日程")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        HrSchedule existing = scheduleService.getById(id);
        if (existing == null) {
            return Result.error("日程不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            return Result.error("只能操作自己的日程");
        }
        boolean ok = scheduleService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.PermissionSupport;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.service.IHrScheduleService;
import com.linan.core.common.api.PageParamGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

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
@RequiredArgsConstructor
public class AppScheduleController {

    private final IHrScheduleService scheduleService;

    private final PermissionSupport permissionSupport;

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

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
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

        String timeError = validateTimeFormat(schedule);
        if (timeError != null) {
            return Result.error(timeError);
        }

        schedule.setUserId(userId);
        if (schedule.getStatus() == null || schedule.getStatus().isEmpty()) {
            schedule.setStatus("0");
        }
        boolean ok = scheduleService.save(schedule);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 时间字段格式校验（统一为 yyyy-MM-dd HH:mm），合法返回 null，非法返回错误消息
     */
    private String validateTimeFormat(HrSchedule schedule) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            java.time.LocalDateTime.parse(schedule.getStartTime(), fmt);
            java.time.LocalDateTime.parse(schedule.getEndTime(), fmt);
        } catch (Exception e) {
            return "时间格式不正确，请使用 yyyy-MM-dd HH:mm 格式";
        }
        if (schedule.getStartTime().compareTo(schedule.getEndTime()) >= 0) {
            return "结束时间必须晚于开始时间";
        }
        // 校验提醒时间格式（如果提供）
        if (schedule.getRemindTime() != null && !schedule.getRemindTime().isEmpty()) {
            try {
                java.time.LocalDateTime.parse(schedule.getRemindTime(), fmt);
            } catch (Exception e) {
                return "提醒时间格式不正确，请使用 yyyy-MM-dd HH:mm 格式";
            }
        }
        return null;
    }

    /**
     * 更新日程（含状态变更：完成/取消）
     * PUT /api/app/v1/schedules
     *
     * <p>字段白名单拷贝：仅允许更新 title/description/startTime/endTime/remindTime/location/status
     * （status 变更即完成/取消入口）；userId/reminderSent 等归属与引擎字段强制以库中原值为准，
     * 防止 mass assignment 篡改。</p>
     */
    @Operation(summary = "更新日程")
    @PutMapping
    public Result<Void> updateSchedule(@RequestBody HrSchedule schedule) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (schedule.getId() == null) {
            return Result.error("日程ID不能为空");
        }
        HrSchedule existing = scheduleService.getById(schedule.getId());
        if (existing == null) {
            return Result.error("日程不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            return Result.error("只能操作自己的日程");
        }

        // 白名单拷贝（null 表示不修改该字段），归属字段 userId 保持 existing 原值不动
        if (schedule.getTitle() != null) {
            if (schedule.getTitle().isBlank()) {
                return Result.error("日程标题不能为空");
            }
            existing.setTitle(schedule.getTitle());
        }
        if (schedule.getDescription() != null) {
            existing.setDescription(schedule.getDescription());
        }
        if (schedule.getStartTime() != null) {
            existing.setStartTime(schedule.getStartTime());
        }
        if (schedule.getEndTime() != null) {
            existing.setEndTime(schedule.getEndTime());
        }
        if (schedule.getRemindTime() != null) {
            existing.setRemindTime(schedule.getRemindTime());
        }
        if (schedule.getLocation() != null) {
            existing.setLocation(schedule.getLocation());
        }
        if (schedule.getStatus() != null) {
            // 状态白名单：0=未开始 1=进行中 2=已完成 3=已取消（与 hr_schedule.status 约定一致）
            if (!List.of("0", "1", "2", "3").contains(schedule.getStatus())) {
                return Result.error("状态值不合法");
            }
            existing.setStatus(schedule.getStatus());
        }

        // 时间变更时复用新增路径的格式校验（以合并后的最终值校验）
        String timeError = validateTimeFormat(existing);
        if (timeError != null) {
            return Result.error(timeError);
        }

        boolean ok = scheduleService.updateById(existing);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除日程（本人 OR 持日程删除权限的管理员，双轨校验）
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
        if (!permissionSupport.isOwnerOrHasPermission(userId, existing.getUserId(), "hr:schedule:delete")) {
            return Result.error("无权限删除该日程");
        }
        boolean ok = scheduleService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

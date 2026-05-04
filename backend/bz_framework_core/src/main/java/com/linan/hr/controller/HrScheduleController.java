package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.hr.entity.HrSchedule;
import com.linan.hr.service.IHrScheduleService;
import com.linan.system.entity.SysUser;
import com.linan.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 日程管理 Controller（管理后台）
 *
 * <p>管理员查看全员个人日程，便于协调排期；支持按用户/时间/状态筛选与删除。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "个人日程", description = "人事管理 - 日程管理")
@RestController
@RequestMapping("/api/hr/v1/schedules")
@RequiredArgsConstructor
public class HrScheduleController {

    private final IHrScheduleService scheduleService;

    private final ISysUserService userService;

    /**
     * 分页查询全员日程（可按用户、状态、日期范围筛选）
     */
    @RequirePermission("hr:schedule:list")
    @Operation(summary = "分页查询全员日程")
    @GetMapping("/page")
    public Result<PageResult<HrSchedule>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Page<HrSchedule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HrSchedule> wrapper = new LambdaQueryWrapper<>();

        if (userName != null && !userName.isEmpty()) {
            wrapper.like(HrSchedule::getUserName, userName);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(HrSchedule::getStatus, status);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(HrSchedule::getStartTime, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(HrSchedule::getStartTime, endDate);
        }
        wrapper.orderByDesc(HrSchedule::getStartTime)
                .orderByDesc(HrSchedule::getCreateTime);
        IPage<HrSchedule> result = scheduleService.page(page, wrapper);

        // 用户端新增日程时未冗余 userName，管理端按 userId 关联 sys_user 回填昵称
        List<HrSchedule> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> userIds = records.stream()
                    .map(HrSchedule::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                Map<Long, SysUser> userMap = userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
                records.forEach(s -> {
                    SysUser u = userMap.get(s.getUserId());
                    if (u != null) {
                        s.setUserName(u.getNickName() != null ? u.getNickName() : u.getUserName());
                    }
                });
            }
        }
        return Result.page(result);
    }

    /**
     * 删除日程（管理端兜底清理，含已离职/误创建的日程）
     */
    @Operation(summary = "删除日程")
    @OperLog(title = "日程管理", businessType = 3)
    @RequirePermission("hr:schedule:delete")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        boolean removed = scheduleService.removeById(id);
        return removed ? Result.success(null) : Result.error("日程不存在或已删除");
    }
}

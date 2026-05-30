package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.PermissionSupport;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrWorkLog;
import com.linan.hr.service.IHrWorkLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 工作日志 Controller（管理后台）
 *
 * <p>员工填写日报/周报，上级/管理员可查看全员日志并批阅。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "工作日志", description = "人事管理 - 工作日志")
@RestController
@RequestMapping("/api/hr/v1/worklogs")
@RequiredArgsConstructor
public class HrWorkLogController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IHrWorkLogService workLogService;

    /** 权限辅助工具 */
    private final PermissionSupport permissionSupport;

    /**
     * 条件分页查询
     */
    @RequirePermission("hr:worklog:list")
    @Operation(summary = "分页查询工作日志")
    @GetMapping("/page")
    public Result<PageResult<HrWorkLog>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean onlyMine) {

        Page<HrWorkLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HrWorkLog> wrapper = new LambdaQueryWrapper<>();

        // 服务端强制权限校验：仅管理员（持 hr:worklog:audit）可按 userId 筛选，否则强制只看本人
        Long currentUserId = AuthInterceptor.getCurrentUserId();
        boolean hasAuditPerm = permissionSupport.hasPermission(currentUserId, "hr:worklog:audit");
        
        if (hasAuditPerm && userId != null) {
            wrapper.eq(HrWorkLog::getUserId, userId);
        } else {
            wrapper.eq(HrWorkLog::getUserId, currentUserId);
        }
        
        if (logType != null && !logType.isEmpty()) {
            wrapper.eq(HrWorkLog::getLogType, logType);
        }
        if (userName != null && !userName.isEmpty()) {
            wrapper.like(HrWorkLog::getUserName, userName);
        }
        wrapper.orderByDesc(HrWorkLog::getLogDate)
                .orderByDesc(HrWorkLog::getCreateTime);
        IPage<HrWorkLog> result = workLogService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增工作日志（作者自动填充为当前登录用户）
     */
    @Operation(summary = "新增工作日志")
    @OperLog(title = "工作日志", businessType = 1)
    @RequirePermission("hr:worklog:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody HrWorkLog log) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        log.setUserId(userId);
        if (log.getUserName() == null || log.getUserName().isEmpty()) {
            log.setUserName(AuthInterceptor.getCurrentUsername());
        }
        boolean ok = workLogService.save(log);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新工作日志
     */
    @Operation(summary = "修改工作日志")
    @OperLog(title = "工作日志", businessType = 2)
    @RequirePermission("hr:worklog:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody HrWorkLog log) {
        boolean ok = workLogService.updateById(log);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除工作日志
     */
    @Operation(summary = "删除工作日志")
    @OperLog(title = "工作日志", businessType = 3)
    @RequirePermission("hr:worklog:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = workLogService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 批阅工作日志
     */
    @Operation(summary = "批阅工作日志")
    @OperLog(title = "工作日志", businessType = 2)
    @RequirePermission("hr:worklog:review")
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id,
                               @RequestParam(required = false) String remark) {
        HrWorkLog log = workLogService.getById(id);
        if (log == null) {
            return Result.error("日志不存在");
        }
        Long reviewerId = AuthInterceptor.getCurrentUserId();
        // 禁止批阅自己的日志（与审批流程"不能审批自己"的规则一致，避免自评自批）
        if (reviewerId != null && reviewerId.equals(log.getUserId())) {
            return Result.error("不能批阅自己的日志");
        }
        // 同一审批人重复批注拒绝：历史批注只增不改，避免静默覆盖
        if (log.getReviewerId() != null && log.getReviewerId().equals(reviewerId)) {
            return Result.error("该日志已批阅，不能重复批注");
        }
        log.setReviewerId(reviewerId);
        log.setReviewerName(AuthInterceptor.getCurrentUsername());
        log.setReviewTime(LocalDateTime.now().format(FMT));
        log.setReviewRemark(remark);
        boolean ok = workLogService.updateById(log);
        return ok ? Result.success(null) : Result.error("批阅失败");
    }
}

package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.service.IAttOvertimeRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 加班申请 Controller
 *
 * <p>流程：员工提交申请（状态0待审批）→ 管理员审批通过/驳回（状态1/2），
 * 待审批状态下申请人可撤销（状态3）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "加班申请", description = "考勤管理 - 加班申请与审批")
@RestController
@RequestMapping("/api/hr/v1/overtimes")
public class AttOvertimeRequestController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private IAttOvertimeRequestService overtimeService;

    /**
     * 分页查询加班记录
     */
    @RequirePermission("hr:overtime:list")
    @Operation(summary = "分页查询加班记录")
    @GetMapping("/page")
    public Result<PageResult<AttOvertimeRequest>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onlyMine) {

        Page<AttOvertimeRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AttOvertimeRequest> wrapper = new LambdaQueryWrapper<>();

        if (Boolean.TRUE.equals(onlyMine)) {
            wrapper.eq(AttOvertimeRequest::getUserId, AuthInterceptor.getCurrentUserId());
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AttOvertimeRequest::getStatus, status);
        }
        wrapper.orderByDesc(AttOvertimeRequest::getCreateTime);
        IPage<AttOvertimeRequest> result = overtimeService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 提交加班申请
     */
    @Operation(summary = "提交加班申请")
    @OperLog(title = "加班申请", businessType = 1)
    @RequirePermission("hr:overtime:add")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody AttOvertimeRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择加班起止时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        request.setUserId(userId);
        if (request.getUserName() == null || request.getUserName().isEmpty()) {
            request.setUserName(String.valueOf(userId));
        }
        request.setStatus("0");
        boolean ok = overtimeService.save(request);
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批加班（通过/驳回）
     */
    @Operation(summary = "审批加班")
    @OperLog(title = "加班申请", businessType = 2)
    @RequirePermission("hr:overtime:approve")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) String remark) {
        AttOvertimeRequest request = overtimeService.getById(id);
        if (request == null) {
            return Result.error("加班记录不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("该记录已审批，不能重复操作");
        }
        Long approverId = AuthInterceptor.getCurrentUserId();
        if (approverId != null && approverId.equals(request.getUserId())) {
            return Result.error("不能审批自己的申请");
        }
        if ("pass".equals(action)) {
            request.setStatus("1");
        } else if ("reject".equals(action)) {
            request.setStatus("2");
        } else {
            return Result.error("无效的审批动作");
        }
        request.setApproverId(approverId);
        request.setApproverName(AuthInterceptor.getCurrentUsername());
        request.setApproveTime(LocalDateTime.now().format(FMT));
        request.setApproveRemark(remark);
        boolean ok = overtimeService.updateById(request);
        return ok ? Result.success(null) : Result.error("审批失败");
    }

    /**
     * 撤销加班申请（仅待审批状态可撤销）
     */
    @Operation(summary = "撤销加班申请")
    @OperLog(title = "加班申请", businessType = 3)
    @RequirePermission("hr:overtime:edit")
    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        AttOvertimeRequest request = overtimeService.getById(id);
        if (request == null) {
            return Result.error("加班记录不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("仅待审批状态的申请可撤销");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null && !userId.equals(request.getUserId())) {
            return Result.error("只能撤销自己的申请");
        }
        request.setStatus("3");
        boolean ok = overtimeService.updateById(request);
        return ok ? Result.success(null) : Result.error("撤销失败");
    }

    /**
     * 删除加班记录
     */
    @Operation(summary = "删除加班记录")
    @OperLog(title = "加班申请", businessType = 3)
    @RequirePermission("hr:overtime:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = overtimeService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

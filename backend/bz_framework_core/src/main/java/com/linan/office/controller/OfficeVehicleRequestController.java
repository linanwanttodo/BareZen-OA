package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeVehicleRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用车申请 Controller
 *
 * <p>流程：员工提交申请（状态0待审批）→ 管理员审批通过/驳回（状态1/2），
 * 待审批状态下申请人可取消（状态3）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "用车申请", description = "行政办公 - 用车申请")
@RestController
@RequestMapping("/api/office/v1/vehicles")
public class OfficeVehicleRequestController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private IOfficeVehicleRequestService vehicleService;

    /**
     * 分页查询用车申请
     */
    @RequirePermission("office:vehicle:list")
    @Operation(summary = "分页查询用车申请")
    @GetMapping("/page")
    public Result<PageResult<OfficeVehicleRequest>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String applicantName) {

        Page<OfficeVehicleRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OfficeVehicleRequest> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(OfficeVehicleRequest::getStatus, status);
        }
        if (applicantName != null && !applicantName.isEmpty()) {
            wrapper.like(OfficeVehicleRequest::getApplicantName, applicantName);
        }
        wrapper.orderByDesc(OfficeVehicleRequest::getCreateTime);
        IPage<OfficeVehicleRequest> result = vehicleService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 提交用车申请（申请人自动填充为当前登录用户）
     */
    @Operation(summary = "提交用车申请")
    @OperLog(title = "用车申请", businessType = 1)
    @RequirePermission("office:vehicle:add")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody OfficeVehicleRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择用车时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        request.setApplicantId(userId);
        if (request.getApplicantName() == null || request.getApplicantName().isEmpty()) {
            request.setApplicantName(String.valueOf(userId));
        }
        request.setStatus("0");
        boolean ok = vehicleService.save(request);
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批用车申请（通过/驳回）
     */
    @Operation(summary = "审批用车申请")
    @OperLog(title = "用车申请", businessType = 2)
    @RequirePermission("office:vehicle:approve")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) String remark) {
        OfficeVehicleRequest request = vehicleService.getById(id);
        if (request == null) {
            return Result.error("申请不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("该申请已审批，不能重复操作");
        }
        if ("pass".equals(action)) {
            request.setStatus("1");
        } else if ("reject".equals(action)) {
            request.setStatus("2");
        } else {
            return Result.error("无效的审批动作");
        }
        Long approverId = AuthInterceptor.getCurrentUserId();
        request.setApproverId(approverId);
        request.setApproverName(approverId != null ? String.valueOf(approverId) : null);
        request.setApproveTime(LocalDateTime.now().format(FMT));
        request.setApproveRemark(remark);
        boolean ok = vehicleService.updateById(request);
        return ok ? Result.success(null) : Result.error("审批失败");
    }

    /**
     * 取消用车申请（仅待审批状态可取消）
     */
    @Operation(summary = "取消用车申请")
    @OperLog(title = "用车申请", businessType = 3)
    @RequirePermission("office:vehicle:edit")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        OfficeVehicleRequest request = vehicleService.getById(id);
        if (request == null) {
            return Result.error("申请不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("仅待审批状态的申请可取消");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null && !userId.equals(request.getApplicantId())) {
            return Result.error("只能取消自己的申请");
        }
        request.setStatus("3");
        boolean ok = vehicleService.updateById(request);
        return ok ? Result.success(null) : Result.error("取消失败");
    }

    /**
     * 删除用车申请
     */
    @Operation(summary = "删除用车申请")
    @OperLog(title = "用车申请", businessType = 3)
    @RequirePermission("office:vehicle:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = vehicleService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

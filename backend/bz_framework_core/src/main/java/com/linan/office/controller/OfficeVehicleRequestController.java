package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeVehicleRequestService;
import com.linan.system.service.IApprovalFlowLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
@RequiredArgsConstructor
public class OfficeVehicleRequestController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IOfficeVehicleRequestService vehicleService;

    private final ApplicationEventPublisher eventPublisher;

    private final IApprovalFlowLogService flowLogService;

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
        // 后端兜底填真实用户名（JWT 中解析），禁止把 userId 当姓名落库
        if (request.getApplicantName() == null || request.getApplicantName().isEmpty()) {
            String username = AuthInterceptor.getCurrentUsername();
            request.setApplicantName(username != null ? username : "用户" + userId);
        }
        request.setStatus("0");
        boolean ok = vehicleService.save(request);
        if (ok) {
            flowLogService.record("vehicle", request.getId(), "提交申请", "0",
                    request.getApplicantId(), request.getApplicantName(), null);
        }
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批用车申请（通过/驳回）
     *
     * <p>原子条件更新（WHERE status='0'）：并发重复审批时仅首个请求生效，
     * 后到者受影响行数为 0 返回"已审批"，避免覆盖首次审批结果。</p>
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

        // 禁止审批人审批自己的申请（避免自审自批）
        Long approverId = AuthInterceptor.getCurrentUserId();
        if (approverId != null && approverId.equals(request.getApplicantId())) {
            return Result.error("不能审批自己的申请");
        }
        // 指定审批人校验（V31）：若申请指定了审批人，仅该审批人可操作
        if (request.getAssigneeId() != null
                && (approverId == null || !approverId.equals(request.getAssigneeId()))) {
            return Result.error("该申请已指定审批人（" + request.getAssigneeName() + "），仅其可审批");
        }

        String targetStatus;
        if ("pass".equals(action)) {
            targetStatus = "1";
        } else if ("reject".equals(action)) {
            targetStatus = "2";
        } else {
            return Result.error("无效的审批动作");
        }

        // 原子条件更新：仅当仍为待审批（status='0'）时生效，并发重复审批后到者受影响行数为 0
        boolean ok = vehicleService.update(new LambdaUpdateWrapper<OfficeVehicleRequest>()
                .eq(OfficeVehicleRequest::getId, id)
                .eq(OfficeVehicleRequest::getStatus, "0")
                .set(OfficeVehicleRequest::getStatus, targetStatus)
                .set(OfficeVehicleRequest::getApproverId, approverId)
                .set(OfficeVehicleRequest::getApproverName, AuthInterceptor.getCurrentUsername())
                .set(OfficeVehicleRequest::getApproveTime, LocalDateTime.now().format(FMT))
                .set(OfficeVehicleRequest::getApproveRemark, remark));
        if (!ok) {
            return Result.error("该申请已审批，不能重复操作");
        }

        String node = "pass".equals(action) ? "审批通过" : "审批驳回";
        flowLogService.record("vehicle", id, node, targetStatus,
                approverId, AuthInterceptor.getCurrentUsername(), remark);
        String result = "pass".equals(action) ? "已通过" : "已驳回";
        eventPublisher.publishEvent(new ApprovalCompletedEvent(
                request.getApplicantId(),
                "用车审批" + result,
                "您的用车申请（" + request.getPurpose() + "，" + request.getStartTime() + " ~ " + request.getEndTime() + "）" + result
                        + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "")));
        return Result.success(null);
    }

    /**
     * 取消用车申请（仅待审批状态可取消）
     *
     * <p>原子条件更新：与审批并发时仅一个操作生效，避免状态被并发覆盖。</p>
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
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null && !userId.equals(request.getApplicantId())) {
            return Result.error("只能取消自己的申请");
        }
        boolean ok = vehicleService.update(new LambdaUpdateWrapper<OfficeVehicleRequest>()
                .eq(OfficeVehicleRequest::getId, id)
                .eq(OfficeVehicleRequest::getStatus, "0")
                .set(OfficeVehicleRequest::getStatus, "3"));
        if (!ok) {
            return Result.error("仅待审批状态的申请可取消");
        }
        flowLogService.record("vehicle", id, "取消申请", "3",
                userId, AuthInterceptor.getCurrentUsername(), null);
        return Result.success(null);
    }

    /**
     * 删除用车申请
     */
    @Operation(summary = "删除用车申请")
    @OperLog(title = "用车申请", businessType = 3)
    @RequirePermission("office:vehicle:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        OfficeVehicleRequest request = vehicleService.getById(id);
        if (request == null) {
            return Result.error("申请不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("仅待审批状态的单据可删除");
        }
        boolean ok = vehicleService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

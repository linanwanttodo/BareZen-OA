package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeVehicleRequestService;
import com.linan.system.service.ApprovalFlowFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /** 审批流程编排门面（V36+：统一事务/引擎/日志/通知） */
    private final ApprovalFlowFacade flowFacade;

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
        request.setStatus(ApprovalStatus.PENDING.getValue());
        boolean ok = vehicleService.save(request);
        if (ok) {
            // 审批编排统一收口：启动首节点 + 记录提交日志（同一事务）
            flowFacade.submit("vehicle", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                    request.getApplicantId(), request.getApplicantName());
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
        Long approverId = AuthInterceptor.getCurrentUserId();
        String approverName = AuthInterceptor.getCurrentUsername();

        // 审批编排统一收口：自审校验 / 引擎推进 / 终态更新 / 日志 / 通知事件
        flowFacade.approve("vehicle", id, approverId, approverName, action, remark, new ApprovalFlowFacade.ApprovalBizHandler() {
            @Override
            public Long applicantId() {
                return request.getApplicantId();
            }

            @Override
            public boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
                return vehicleService.update(new LambdaUpdateWrapper<OfficeVehicleRequest>()
                        .eq(OfficeVehicleRequest::getId, id)
                        .eq(OfficeVehicleRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(OfficeVehicleRequest::getStatus, targetStatus)
                        .set(OfficeVehicleRequest::getApproverId, approverId)
                        .set(OfficeVehicleRequest::getApproverName, approverName)
                        .set(OfficeVehicleRequest::getApproveTime, LocalDateTime.now().format(FMT))
                        .set(OfficeVehicleRequest::getApproveRemark, remark));
            }

            @Override
            public boolean cancel() {
                return vehicleService.update(new LambdaUpdateWrapper<OfficeVehicleRequest>()
                        .eq(OfficeVehicleRequest::getId, id)
                        .eq(OfficeVehicleRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(OfficeVehicleRequest::getStatus, ApprovalStatus.CANCELED.getValue()));
            }

            @Override
            public ApprovalFlowFacade.NotifyText resultNotify(boolean approved, String remark) {
                String res = approved ? "已通过" : "已驳回";
                String content = "您的用车申请（" + request.getPurpose() + "，" + request.getStartTime() + " ~ "
                        + request.getEndTime() + "）" + res
                        + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "");
                return ApprovalFlowFacade.NotifyText.of("用车审批" + res, content);
            }

            @Override
            public ApprovalFlowFacade.NotifyText nextNotify() {
                return ApprovalFlowFacade.NotifyText.of("新的待审批申请",
                        "有新的用车申请（" + request.getStartTime() + " ~ " + request.getEndTime() + "）待您审批");
            }
        });
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

        // 审批编排统一收口：非本人校验 / 业务表 0→3 原子更新 / 引擎节点取消 / 撤销日志（同一事务）
        flowFacade.revoke("vehicle", id, userId, AuthInterceptor.getCurrentUsername(),
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return request.getApplicantId();
                    }

                    @Override
                    public boolean cancel() {
                        return vehicleService.update(new LambdaUpdateWrapper<OfficeVehicleRequest>()
                                .eq(OfficeVehicleRequest::getId, id)
                                .eq(OfficeVehicleRequest::getStatus, ApprovalStatus.PENDING.getValue())
                                .set(OfficeVehicleRequest::getStatus, ApprovalStatus.CANCELED.getValue()));
                    }

                    @Override
                    public String currentStatus() {
                        return request.getStatus();
                    }
                });
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

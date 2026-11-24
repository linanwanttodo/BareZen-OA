package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeVehicleRequestService;
import com.linan.system.service.ApprovalFlowFacade;
import com.linan.core.common.api.PageParamGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用车申请 Controller（用户前台 /api/app/v1/vehicles）
 *
 * <p>员工自助提交用车申请、查询我的申请与详情、撤销待审批申请。
 * 与管理后台的用车接口（/api/office/v1/vehicles）分离：这里仅需登录即可使用，
 * 申请人自动填充为当前登录用户。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "用车申请", description = "用户前台 - 用车申请")
@RestController
@RequestMapping("/api/app/v1/vehicles")
@RequiredArgsConstructor
public class AppVehicleController {

    private final IOfficeVehicleRequestService vehicleService;

    /** 审批流程编排门面（统一 submit/revoke 编排并保证事务一致） */
    private final ApprovalFlowFacade flowFacade;

    /**
     * 我的用车申请列表
     * GET /api/app/v1/vehicles
     */
    @Operation(summary = "我的用车申请列表")
    @GetMapping
    public Result<PageResult<OfficeVehicleRequest>> myRequests(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<OfficeVehicleRequest> wrapper = new LambdaQueryWrapper<OfficeVehicleRequest>()
                .eq(OfficeVehicleRequest::getApplicantId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OfficeVehicleRequest::getStatus, status);
        }
        wrapper.orderByDesc(OfficeVehicleRequest::getCreateTime);
        IPage<OfficeVehicleRequest> result = vehicleService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 用车申请详情
     * GET /api/app/v1/vehicles/{id}
     */
    @Operation(summary = "用车申请详情")
    @GetMapping("/{id}")
    public Result<OfficeVehicleRequest> detail(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        OfficeVehicleRequest request = vehicleService.getById(id);
        if (request == null) {
            return Result.error("记录不存在");
        }
        // 归属校验：仅允许查看本人提交的申请
        if (!userId.equals(request.getApplicantId())) {
            return Result.error("无权查看该申请");
        }
        return Result.success(request);
    }

    /**
     * 提交用车申请
     * POST /api/app/v1/vehicles
     */
    @Operation(summary = "提交用车申请")
    @OperLog(title = "用车申请", businessType = 1)
    @Transactional(rollbackFor = Exception.class)
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody OfficeVehicleRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择用车时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        // 强制清空客户端可能预置的敏感字段（防主键冲突、审批字段污染）
        request.setId(null);
        request.setApproverId(null);
        request.setApproverName(null);
        request.setApproveTime(null);
        request.setApproveRemark(null);
        // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
        request.setApplicantId(userId);
        String username = AuthInterceptor.getCurrentUsername();
        request.setApplicantName(username != null ? username : "用户" + userId);
        request.setStatus(ApprovalStatus.PENDING.getValue());
        boolean ok = vehicleService.save(request);
        if (!ok) {
            return Result.error("提交失败");
        }
        // 审批编排统一收口：启动引擎首节点 + 记录提交日志（同一事务）。
        // submit 抛异常时向上传播，整个事务回滚（包括已保存的用车记录）
        flowFacade.submit("vehicle", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                userId, request.getApplicantName());
        return Result.success(null);
    }

    /**
     * 撤销我的用车申请
     * PUT /api/app/v1/vehicles/{id}/revoke
     *
     * <p>原子条件更新：与审批并发（如管理员同时审批）时仅一个操作生效，
     * 撤销或审批先到先得，避免状态被并发覆盖。</p>
     */
    @Operation(summary = "撤销用车申请")
    @OperLog(title = "用车申请", businessType = 3)
    @PutMapping("/{id}/revoke")
    public Result<Void> revokeRequest(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        OfficeVehicleRequest request = vehicleService.getById(id);
        if (request == null) {
            return Result.error("记录不存在");
        }
        // 审批编排统一收口：归属校验 / 业务表 0→3 原子更新 / 引擎节点取消 / 撤销日志（同一事务）
        flowFacade.revoke("vehicle", id, userId, request.getApplicantName(),
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
}

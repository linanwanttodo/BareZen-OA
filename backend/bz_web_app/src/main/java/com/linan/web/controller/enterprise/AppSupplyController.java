package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.ApprovalFlowFacade;
import com.linan.core.common.api.PageParamGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 办公用品申领 Controller（用户前台 /api/app/v1/supplies）
 *
 * <p>员工自助查看可申领用品、提交申领、查询我的申领记录、撤销待审批申领。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "办公用品申领", description = "用户前台 - 用品申领")
@RestController
@RequestMapping("/api/app/v1/supplies")
@RequiredArgsConstructor
public class AppSupplyController {

    private final IOfficeSupplyService supplyService;

    private final IOfficeSupplyRequestService requestService;

    private final ApprovalFlowFacade flowFacade;

    /**
     * 可申领用品列表
     * GET /api/app/v1/supplies
     */
    @Operation(summary = "可申领用品列表")
    @GetMapping
    public Result<PageResult<OfficeSupply>> supplies(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        LambdaQueryWrapper<OfficeSupply> wrapper = new LambdaQueryWrapper<OfficeSupply>()
                .eq(OfficeSupply::getStatus, "0")
                .gt(OfficeSupply::getStock, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(OfficeSupply::getSupplyName, keyword);
        }
        wrapper.orderByDesc(OfficeSupply::getCreateTime);
        IPage<OfficeSupply> result = supplyService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 我的申领记录
     * GET /api/app/v1/supplies/requests
     */
    @Operation(summary = "我的申领记录")
    @GetMapping("/requests")
    public Result<PageResult<OfficeSupplyRequest>> myRequests(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<OfficeSupplyRequest> wrapper = new LambdaQueryWrapper<OfficeSupplyRequest>()
                .eq(OfficeSupplyRequest::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OfficeSupplyRequest::getStatus, status);
        }
        wrapper.orderByDesc(OfficeSupplyRequest::getCreateTime);
        IPage<OfficeSupplyRequest> result = requestService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 提交申领
     * POST /api/app/v1/supplies/requests
     */
    @Operation(summary = "提交申领")
    @PostMapping("/requests")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> submitRequest(@Valid @RequestBody OfficeSupplyRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (request.getSupplyId() == null) {
            return Result.error("请选择用品");
        }
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            return Result.error("申领数量至少为 1");
        }
        OfficeSupply supply = supplyService.getById(request.getSupplyId());
        if (supply == null) {
            return Result.error("用品不存在");
        }
        if (supply.getStock() < request.getQuantity()) {
            return Result.error("库存不足，当前库存 " + supply.getStock());
        }
        request.setSupplyName(supply.getSupplyName());
        // 强制清空客户端可能预置的敏感字段（防主键冲突、审批字段污染）
        request.setId(null);
        request.setApproverId(null);
        request.setApproverName(null);
        request.setApproveTime(null);
        request.setApproveRemark(null);
        // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
        request.setUserId(userId);
        request.setUserName(AuthInterceptor.getCurrentUsername());
        request.setStatus("0");
        boolean ok = requestService.save(request);
        if (!ok) {
            return Result.error("提交失败");
        }
        // 审批编排统一收口：启动引擎首节点 + 记录提交日志（同一事务）。
        // submit 抛异常时向上传播，整个事务回滚（包括已保存的申领记录）
        flowFacade.submit("supply", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                userId, request.getUserName());
        return Result.success(null);
    }

    /**
     * 撤销我的申领
     * PUT /api/app/v1/supplies/requests/{id}/revoke
     *
     * <p>原子条件更新：与审批并发时仅一个操作生效，避免状态被并发覆盖。</p>
     */
    @Operation(summary = "撤销申领")
    @OperLog(title = "用品申领", businessType = 3)
    @PutMapping("/requests/{id}/revoke")
    public Result<Void> revokeRequest(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        OfficeSupplyRequest request = requestService.getById(id);
        if (request == null) {
            return Result.error("记录不存在");
        }
        // 审批编排统一收口：归属校验 / 业务表 0→3 原子更新 / 引擎节点取消 / 撤销日志（同一事务）
        flowFacade.revoke("supply", id, userId, request.getUserName(),
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return request.getUserId();
                    }

                    @Override
                    public boolean cancel() {
                        return requestService.update(new LambdaUpdateWrapper<OfficeSupplyRequest>()
                                .eq(OfficeSupplyRequest::getId, id)
                                .eq(OfficeSupplyRequest::getStatus, "0")
                                .set(OfficeSupplyRequest::getStatus, "3"));
                    }

                    @Override
                    public String currentStatus() {
                        return request.getStatus();
                    }
                });
        return Result.success(null);
    }
}

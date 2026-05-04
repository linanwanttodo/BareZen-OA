package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.IApprovalFlowLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
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

    private static final Logger log = LoggerFactory.getLogger(AppSupplyController.class);

    private final IOfficeSupplyService supplyService;

    private final IOfficeSupplyRequestService requestService;

    private final IApprovalFlowLogService flowLogService;

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

        try {
            LambdaQueryWrapper<OfficeSupply> wrapper = new LambdaQueryWrapper<OfficeSupply>()
                    .eq(OfficeSupply::getStatus, "0")
                    .gt(OfficeSupply::getStock, 0);
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.like(OfficeSupply::getSupplyName, keyword);
            }
            wrapper.orderByDesc(OfficeSupply::getCreateTime);
            IPage<OfficeSupply> result = supplyService.page(new Page<>(page, pageSize), wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询用品列表失败", e);
            return Result.error("查询失败");
        }
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

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            LambdaQueryWrapper<OfficeSupplyRequest> wrapper = new LambdaQueryWrapper<OfficeSupplyRequest>()
                    .eq(OfficeSupplyRequest::getUserId, userId);
            if (status != null && !status.isEmpty()) {
                wrapper.eq(OfficeSupplyRequest::getStatus, status);
            }
            wrapper.orderByDesc(OfficeSupplyRequest::getCreateTime);
            IPage<OfficeSupplyRequest> result = requestService.page(new Page<>(page, pageSize), wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询我的申领记录失败, userId={}", userId, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 提交申领
     * POST /api/app/v1/supplies/requests
     */
    @Operation(summary = "提交申领")
    @PostMapping("/requests")
    public Result<Void> submitRequest(@RequestBody OfficeSupplyRequest request) {
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
        try {
            request.setSupplyName(supply.getSupplyName());
            request.setUserId(userId);
            request.setStatus("0");
            boolean ok = requestService.save(request);
            if (ok) {
                flowLogService.record("supply", request.getId(), "提交申请", "0",
                        userId, request.getUserName(), null);
            }
            return ok ? Result.success(null) : Result.error("提交失败");
        } catch (Exception e) {
            log.error("提交申领失败, userId={}", userId, e);
            return Result.error("提交失败");
        }
    }

    /**
     * 撤销我的申领
     * PUT /api/app/v1/supplies/requests/{id}/revoke
     */
    @Operation(summary = "撤销申领")
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
        if (!userId.equals(request.getUserId())) {
            return Result.error("只能撤销自己的申领");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("仅待审批状态的申领可撤销");
        }
        request.setStatus("3");
        boolean ok = requestService.updateById(request);
        if (ok) {
            flowLogService.record("supply", request.getId(), "撤销申请", "3",
                    userId, request.getUserName(), null);
        }
        return ok ? Result.success(null) : Result.error("撤销失败");
    }
}

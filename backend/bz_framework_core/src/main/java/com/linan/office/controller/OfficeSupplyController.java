package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.IApprovalFlowLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 办公用品管理 Controller（管理后台）
 *
 * <p>用品入库/维护/停用；申领记录审批（通过/驳回）。
 * 审批通过时自动扣减库存（库存不足则拒绝）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "办公用品", description = "行政办公 - 用品管理")
@RestController
@RequestMapping("/api/office/v1/supplies")
public class OfficeSupplyController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private IOfficeSupplyService supplyService;

    @Autowired
    private IOfficeSupplyRequestService requestService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private IApprovalFlowLogService flowLogService;

    // ============ 用品管理 ============

    /**
     * 用品分页查询
     */
    @RequirePermission("office:supply:list")
    @Operation(summary = "分页查询办公用品")
    @GetMapping("/page")
    public Result<PageResult<OfficeSupply>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String supplyName,
            @RequestParam(required = false) String category) {

        Page<OfficeSupply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OfficeSupply> wrapper = new LambdaQueryWrapper<>();

        if (supplyName != null && !supplyName.isEmpty()) {
            wrapper.like(OfficeSupply::getSupplyName, supplyName);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(OfficeSupply::getCategory, category);
        }
        wrapper.orderByDesc(OfficeSupply::getCreateTime);
        IPage<OfficeSupply> result = supplyService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增用品（入库）
     */
    @Operation(summary = "新增办公用品")
    @OperLog(title = "办公用品", businessType = 1)
    @RequirePermission("office:supply:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody OfficeSupply supply) {
        if (supply.getStock() == null) {
            supply.setStock(0);
        }
        if (supply.getStatus() == null || supply.getStatus().isEmpty()) {
            supply.setStatus("0");
        }
        boolean ok = supplyService.save(supply);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新用品
     */
    @Operation(summary = "修改办公用品")
    @OperLog(title = "办公用品", businessType = 2)
    @RequirePermission("office:supply:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody OfficeSupply supply) {
        boolean ok = supplyService.updateById(supply);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除用品
     */
    @Operation(summary = "删除办公用品")
    @OperLog(title = "办公用品", businessType = 3)
    @RequirePermission("office:supply:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = supplyService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    // ============ 申领审批 ============

    /**
     * 申领记录分页查询
     */
    @RequirePermission("office:supply:list")
    @Operation(summary = "分页查询申领记录")
    @GetMapping("/requests/page")
    public Result<PageResult<OfficeSupplyRequest>> requestList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {

        Page<OfficeSupplyRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OfficeSupplyRequest> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(OfficeSupplyRequest::getStatus, status);
        }
        wrapper.orderByDesc(OfficeSupplyRequest::getCreateTime);
        IPage<OfficeSupplyRequest> result = requestService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 审批申领（通过/驳回），通过时扣减库存
     */
    @Operation(summary = "审批申领")
    @OperLog(title = "办公用品", businessType = 2)
    @RequirePermission("office:supply:approve")
    @PutMapping("/requests/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) String remark) {
        OfficeSupplyRequest request = requestService.getById(id);
        if (request == null) {
            return Result.error("申领记录不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("该记录已审批，不能重复操作");
        }
        Long approverId = AuthInterceptor.getCurrentUserId();
        if (approverId != null && approverId.equals(request.getUserId())) {
            return Result.error("不能审批自己的申请");
        }

        if ("pass".equals(action)) {
            // 校验并扣减库存
            OfficeSupply supply = supplyService.getById(request.getSupplyId());
            if (supply == null) {
                return Result.error("用品已不存在");
            }
            if (supply.getStock() < request.getQuantity()) {
                return Result.error("库存不足，当前库存 " + supply.getStock());
            }
            supply.setStock(supply.getStock() - request.getQuantity());
            supplyService.updateById(supply);
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
        boolean ok = requestService.updateById(request);
        if (ok) {
            String node = "pass".equals(action) ? "审批通过" : "审批驳回";
            flowLogService.record("supply", request.getId(), node, request.getStatus(),
                    approverId, AuthInterceptor.getCurrentUsername(), remark);
            String result = "pass".equals(action) ? "已通过" : "已驳回";
            eventPublisher.publishEvent(new ApprovalCompletedEvent(
                    request.getUserId(),
                    "用品申领审批" + result,
                    "您的用品申领（" + request.getSupplyName() + " × " + request.getQuantity() + "）" + result
                            + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "")));
        }
        return ok ? Result.success(null) : Result.error("审批失败");
    }

    /**
     * 删除申领记录
     */
    @Operation(summary = "删除申领记录")
    @OperLog(title = "办公用品", businessType = 3)
    @RequirePermission("office:supply:delete")
    @DeleteMapping("/requests/{id}")
    public Result<Void> deleteRequest(@PathVariable Long id) {
        boolean ok = requestService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

package com.linan.office.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

    private final IOfficeSupplyService supplyService;

    private final IOfficeSupplyRequestService requestService;

    public OfficeSupplyController(IOfficeSupplyService supplyService,
                                  IOfficeSupplyRequestService requestService) {
        this.supplyService = supplyService;
        this.requestService = requestService;
    }

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
     *
     * <p>业务逻辑（行锁/库存扣减/流程记录）与事务已下沉至 Service，
     * 保证库存扣减与状态变更的整体原子性。</p>
     */
    @Operation(summary = "审批申领")
    @OperLog(title = "办公用品", businessType = 2)
    @RequirePermission("office:supply:approve")
    @PutMapping("/requests/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) String remark) {
        requestService.approve(id, action, remark);
        return Result.success(null);
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

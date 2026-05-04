package com.linan.asset.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.service.IAssetItemService;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

/**
 * 资产台账 Controller
 *
 * <p>支持资产全生命周期：新增（入库）、编辑（含领用/状态变更）、报废、删除。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "资产台账", description = "资产管理 - 固定资产台账")
@RestController
@RequestMapping("/api/asset/v1/items")
@RequiredArgsConstructor
public class AssetItemController {

    private final IAssetItemService assetService;

    /**
     * 条件分页查询
     */
    @RequirePermission("asset:item:list")
    @Operation(summary = "分页查询资产台账")
    @GetMapping("/page")
    public Result<PageResult<AssetItem>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String assetName,
            @RequestParam(required = false) String assetNo,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String holderName) {

        Page<AssetItem> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AssetItem> wrapper = new LambdaQueryWrapper<>();

        if (assetName != null && !assetName.isEmpty()) {
            wrapper.like(AssetItem::getAssetName, assetName);
        }
        if (assetNo != null && !assetNo.isEmpty()) {
            wrapper.like(AssetItem::getAssetNo, assetNo);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(AssetItem::getCategory, category);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AssetItem::getStatus, status);
        }
        if (holderName != null && !holderName.isEmpty()) {
            wrapper.like(AssetItem::getHolderName, holderName);
        }

        wrapper.orderByDesc(AssetItem::getCreateTime);
        IPage<AssetItem> result = assetService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增资产（入库）
     */
    @Operation(summary = "新增资产")
    @OperLog(title = "资产台账", businessType = 1)
    @RequirePermission("asset:item:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody AssetItem item) {
        if (item.getStatus() == null || item.getStatus().isEmpty()) {
            item.setStatus("0");
        }
        boolean ok = assetService.save(item);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新资产（含领用：设置使用人/部门/状态；报废：设置报废日期/状态）
     */
    @Operation(summary = "修改资产")
    @OperLog(title = "资产台账", businessType = 2)
    @RequirePermission("asset:item:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AssetItem item) {
        boolean ok = assetService.updateById(item);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除资产
     */
    @Operation(summary = "删除资产")
    @OperLog(title = "资产台账", businessType = 3)
    @RequirePermission("asset:item:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = assetService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 资产报废（状态置为已报废并记录报废日期）
     */
    @Operation(summary = "资产报废")
    @OperLog(title = "资产台账", businessType = 3)
    @RequirePermission("asset:item:edit")
    @PutMapping("/{id}/scrap")
    public Result<Void> scrap(@PathVariable Long id) {
        AssetItem item = assetService.getById(id);
        if (item == null) {
            return Result.error("资产不存在");
        }
        if ("3".equals(item.getStatus())) {
            return Result.error("该资产已报废");
        }
        item.setStatus("3");
        item.setScrapDate(LocalDate.now().toString());
        boolean ok = assetService.updateById(item);
        return ok ? Result.success(null) : Result.error("报废失败");
    }
}

package com.linan.asset.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.asset.entity.AssetBorrowRecord;
import com.linan.asset.entity.AssetItem;
import com.linan.asset.service.IAssetBorrowRecordService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private final IAssetBorrowRecordService borrowRecordService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            @RequestParam(required = false) Long categoryId,
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
        if (categoryId != null) {
            wrapper.eq(AssetItem::getCategoryId, categoryId);
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

    /**
     * 借用资产（仅在库可借；借用中不可重复借）
     * PUT /api/asset/v1/items/{id}/borrow
     *
     * <p>原子条件更新（WHERE status='0'）：并发借阅仅首个生效。</p>
     */
    @Operation(summary = "借用资产")
    @OperLog(title = "资产台账", businessType = 2)
    @RequirePermission("asset:item:borrow")
    @PutMapping("/{id}/borrow")
    public Result<Void> borrow(@PathVariable Long id,
                               @RequestParam(required = false) String borrowerName,
                               @RequestParam(required = false) String expectReturnTime,
                               @RequestParam(required = false) String remark) {
        AssetItem item = assetService.getById(id);
        if (item == null) {
            return Result.error("资产不存在");
        }
        if (!"0".equals(item.getStatus())) {
            return Result.error("仅限在库资产借用（当前状态：" + item.getStatus() + "）");
        }
        // 在库状态原子流转为使用中，防止并发重复借用
        boolean ok = assetService.update(new LambdaUpdateWrapper<AssetItem>()
                .eq(AssetItem::getId, id)
                .eq(AssetItem::getStatus, "0")
                .set(AssetItem::getStatus, "1")
                .set(AssetItem::getHolderName, borrowerName));
        if (!ok) {
            return Result.error("该资产已被并发借用，请刷新后重试");
        }
        AssetBorrowRecord record = new AssetBorrowRecord();
        record.setAssetId(id);
        record.setBorrowerName(borrowerName);
        record.setBorrowTime(LocalDateTime.now().format(FMT));
        record.setExpectReturnTime(expectReturnTime);
        record.setRemark(remark);
        record.setStatus("0");
        borrowRecordService.save(record);
        return Result.success(null);
    }

    /**
     * 归还资产（借用中才可归还，归还置回在库并记录归还时间）
     * PUT /api/asset/v1/items/{id}/return
     */
    @Operation(summary = "归还资产")
    @OperLog(title = "资产台账", businessType = 2)
    @RequirePermission("asset:item:borrow")
    @PutMapping("/{id}/return")
    public Result<Void> returnAsset(@PathVariable Long id) {
        AssetItem item = assetService.getById(id);
        if (item == null) {
            return Result.error("资产不存在");
        }
        if (!"1".equals(item.getStatus())) {
            return Result.error("仅使用中的资产可归还");
        }
        boolean ok = assetService.update(new LambdaUpdateWrapper<AssetItem>()
                .eq(AssetItem::getId, id)
                .eq(AssetItem::getStatus, "1")
                .set(AssetItem::getStatus, "0")
                .set(AssetItem::getHolderName, (String) null));
        if (!ok) {
            return Result.error("该资产状态已变更，归还失败");
        }
        // 最新借用中记录置为已归还
        borrowRecordService.update(new LambdaUpdateWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getAssetId, id)
                .eq(AssetBorrowRecord::getStatus, "0")
                .set(AssetBorrowRecord::getStatus, "1")
                .set(AssetBorrowRecord::getReturnTime, LocalDateTime.now().format(FMT)));
        return Result.success(null);
    }

    /**
     * 资产的借用记录（时间倒序）
     * GET /api/asset/v1/items/{id}/borrow-records
     */
    @Operation(summary = "查询资产借用记录")
    @RequirePermission("asset:item:list")
    @GetMapping("/{id}/borrow-records")
    public Result<List<AssetBorrowRecord>> borrowRecords(@PathVariable Long id) {
        return Result.success(borrowRecordService.list(new LambdaQueryWrapper<AssetBorrowRecord>()
                .eq(AssetBorrowRecord::getAssetId, id)
                .orderByDesc(AssetBorrowRecord::getCreateTime)));
    }
}

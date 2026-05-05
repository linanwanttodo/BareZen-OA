package com.linan.asset.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.service.IAssetCategoryService;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 资产分类 Controller（管理后台）
 *
 * <p>V39 新增：分类树管理（替代 V11 硬编码枚举），资产台账页分类下拉改查本接口。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "资产分类", description = "资产管理 - 资产分类树")
@RestController
@RequestMapping("/api/asset/v1/categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final IAssetCategoryService categoryService;

    /**
     * 分类列表（全部，前端组树；可按启用状态过滤）
     * GET /api/asset/v1/categories?status=1
     */
    @Operation(summary = "查询资产分类列表")
    @RequirePermission("asset:category:list")
    @GetMapping
    public Result<List<AssetCategory>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AssetCategory::getStatus, status);
        }
        wrapper.orderByAsc(AssetCategory::getSort)
                .orderByAsc(AssetCategory::getId);
        return Result.success(categoryService.list(wrapper));
    }

    /**
     * 新增分类
     */
    @Operation(summary = "新增资产分类")
    @OperLog(title = "资产分类", businessType = 1)
    @RequirePermission("asset:category:add")
    @PostMapping
    public Result<Void> save(@RequestBody AssetCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        // 同名唯一（uk_name）
        long exists = categoryService.count(new LambdaQueryWrapper<AssetCategory>()
                .eq(AssetCategory::getName, category.getName()));
        if (exists > 0) {
            throw new BusinessException("分类名称已存在");
        }
        boolean ok = categoryService.save(category);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 修改分类
     */
    @Operation(summary = "修改资产分类")
    @OperLog(title = "资产分类", businessType = 2)
    @RequirePermission("asset:category:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AssetCategory category) {
        category.setId(id);
        boolean ok = categoryService.updateById(category);
        return ok ? Result.success(null) : Result.error("分类不存在或更新失败");
    }

    /**
     * 删除分类（逻辑删除；有关联资产的分类禁止删除）
     */
    @Operation(summary = "删除资产分类")
    @OperLog(title = "资产分类", businessType = 3)
    @RequirePermission("asset:category:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = categoryService.removeById(id);
        return ok ? Result.success(null) : Result.error("分类不存在");
    }
}
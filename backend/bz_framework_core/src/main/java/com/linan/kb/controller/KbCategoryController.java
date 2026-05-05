package com.linan.kb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.RequirePermission;
import com.linan.kb.entity.KbCategory;
import com.linan.kb.service.IKbCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 知识库分类 Controller（管理后台）
 *
 * <p>V41 新增：分类树管理（替代 V13 硬编码枚举），知识文档页分类下拉改查本接口。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "知识库分类", description = "知识库 - 分类树")
@RestController
@RequestMapping("/api/kb/v1/categories")
@RequiredArgsConstructor
public class KbCategoryController {

    private final IKbCategoryService categoryService;

    /**
     * 分类列表（全部，前端组树；可按启用状态过滤）
     * GET /api/kb/v1/categories?status=1
     */
    @Operation(summary = "查询知识库分类列表")
    @RequirePermission("kb:category:list")
    @GetMapping
    public Result<List<KbCategory>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(KbCategory::getStatus, status);
        }
        wrapper.orderByAsc(KbCategory::getSort)
                .orderByAsc(KbCategory::getId);
        return Result.success(categoryService.list(wrapper));
    }

    /**
     * 新增分类
     */
    @Operation(summary = "新增知识库分类")
    @OperLog(title = "知识库分类", businessType = 1)
    @RequirePermission("kb:category:add")
    @PostMapping
    public Result<Void> save(@RequestBody KbCategory category) {
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
        long exists = categoryService.count(new LambdaQueryWrapper<KbCategory>()
                .eq(KbCategory::getName, category.getName()));
        if (exists > 0) {
            throw new BusinessException("分类名称已存在");
        }
        boolean ok = categoryService.save(category);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 修改分类
     */
    @Operation(summary = "修改知识库分类")
    @OperLog(title = "知识库分类", businessType = 2)
    @RequirePermission("kb:category:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody KbCategory category) {
        category.setId(id);
        boolean ok = categoryService.updateById(category);
        return ok ? Result.success(null) : Result.error("分类不存在或更新失败");
    }

    /**
     * 删除分类（逻辑删除）
     */
    @Operation(summary = "删除知识库分类")
    @OperLog(title = "知识库分类", businessType = 3)
    @RequirePermission("kb:category:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = categoryService.removeById(id);
        return ok ? Result.success(null) : Result.error("分类不存在");
    }
}
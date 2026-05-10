package com.linan.asset.controller;

import com.linan.asset.dto.AssetCategoryDTO;
import com.linan.asset.entity.AssetCategory;
import com.linan.asset.service.IAssetCategoryService;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产分类 Controller（管理后台）
 *
 * <p>V39 新增：分类树管理（替代 V11 硬编码枚举），资产台账页分类下拉改查本接口。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "资产分类", description = "资产管理 - 资产分类树管理接口")
@RestController
@RequestMapping("/api/asset/v1/categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final IAssetCategoryService categoryService;

    /**
     * 分类列表（全部，前端组树；可按启用状态过滤）
     * GET /api/asset/v1/categories?status=1
     */
    @Operation(summary = "查询资产分类列表", description = "获取所有资产分类，支持按启用状态过滤，返回结果按排序字段和ID升序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @RequirePermission("asset:category:list")
    @GetMapping
    public Result<List<AssetCategory>> list(
            @Parameter(description = "启用状态：1=启用，0=禁用，不传则查询全部")
            @RequestParam(required = false) Integer status) {
        return Result.success(categoryService.listCategories(status));
    }

    /**
     * 新增分类
     */
    @Operation(summary = "新增资产分类", description = "创建新的资产分类，分类名称不能重复")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "参数错误或分类名称已存在"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @OperLog(title = "资产分类", businessType = 1)
    @RequirePermission("asset:category:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody AssetCategoryDTO categoryDTO) {
        // DTO转Entity
        AssetCategory category = new AssetCategory();
        category.setName(categoryDTO.getName());
        category.setParentId(categoryDTO.getParentId());
        category.setSort(categoryDTO.getSort());
        category.setStatus(categoryDTO.getStatus());
        
        categoryService.addCategory(category);
        return Result.success(null);
    }

    /**
     * 修改分类
     */
    @Operation(summary = "修改资产分类", description = "更新已有资产分类信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @OperLog(title = "资产分类", businessType = 2)
    @RequirePermission("asset:category:edit")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AssetCategoryDTO categoryDTO) {
        // DTO转Entity
        AssetCategory category = new AssetCategory();
        category.setId(id);
        category.setName(categoryDTO.getName());
        category.setParentId(categoryDTO.getParentId());
        category.setSort(categoryDTO.getSort());
        category.setStatus(categoryDTO.getStatus());
        
        categoryService.updateCategory(category);
        return Result.success(null);
    }

    /**
     * 删除分类（逻辑删除；有关联资产的分类禁止删除）
     */
    @Operation(summary = "删除资产分类", description = "逻辑删除指定分类，如果该分类下有关联资产则无法删除")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限"),
            @ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @OperLog(title = "资产分类", businessType = 3)
    @RequirePermission("asset:category:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "分类ID", required = true)
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success(null);
    }
}
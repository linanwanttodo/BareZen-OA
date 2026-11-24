package com.linan.plugin.masterdata.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.masterdata.entity.MaterialInfo;
import com.linan.plugin.masterdata.service.IMaterialInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 物料信息Controller
 *
 * @author BareZen Team
 */
@Tag(name = "主数据-物料", description = "物料信息管理接口")
@RestController
@RequestMapping("/api/masterdata/v1/materials")
@RequiredArgsConstructor
public class MaterialInfoController {

    private final IMaterialInfoService materialInfoService;

    @Operation(summary = "分页查询物料")
    @RequirePermission("masterdata:material:list")
    @GetMapping("/page")
    public Result<PageResult<MaterialInfo>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "编码/名称模糊关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态: 0=正常, 1=停用") @RequestParam(required = false) String status) {
        LambdaQueryWrapper<MaterialInfo> wrapper = new LambdaQueryWrapper<MaterialInfo>()
                .eq(status != null && !status.isBlank(), MaterialInfo::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(MaterialInfo::getMaterialCode, keyword)
                        .or()
                        .like(MaterialInfo::getMaterialName, keyword))
                .orderByDesc(MaterialInfo::getUpdateTime);
        return Result.page(materialInfoService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询物料详情")
    @RequirePermission("masterdata:material:list")
    @GetMapping("/{id}")
    public Result<MaterialInfo> getById(
            @Parameter(description = "物料ID", required = true)
            @PathVariable Long id) {
        return Result.success(materialInfoService.getById(id));
    }

    @Operation(summary = "新增物料")
    @RequirePermission("masterdata:material:add")
    @PostMapping
    public Result<MaterialInfo> save(@RequestBody MaterialInfo material) {
        return Result.success(materialInfoService.createMaterial(material));
    }

    @Operation(summary = "更新物料")
    @RequirePermission("masterdata:material:edit")
    @PutMapping
    public Result<MaterialInfo> update(@RequestBody MaterialInfo material) {
        return Result.success(materialInfoService.updateMaterial(material));
    }

    @Operation(summary = "删除物料")
    @RequirePermission("masterdata:material:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "物料ID", required = true)
            @PathVariable Long id) {
        boolean ok = materialInfoService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

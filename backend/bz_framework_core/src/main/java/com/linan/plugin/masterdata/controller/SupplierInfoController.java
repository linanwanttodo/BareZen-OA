package com.linan.plugin.masterdata.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.masterdata.entity.SupplierInfo;
import com.linan.plugin.masterdata.service.ISupplierInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商信息Controller
 *
 * @author BareZen Team
 */
@Tag(name = "主数据-供应商", description = "供应商信息管理接口")
@RestController
@RequestMapping("/api/masterdata/v1/suppliers")
@RequiredArgsConstructor
public class SupplierInfoController {

    private final ISupplierInfoService supplierInfoService;

    @Operation(summary = "分页查询供应商")
    @RequirePermission("masterdata:supplier:list")
    @GetMapping("/page")
    public Result<PageResult<SupplierInfo>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "编码/名称模糊关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态: 0=正常, 1=停用") @RequestParam(required = false) String status) {
        LambdaQueryWrapper<SupplierInfo> wrapper = new LambdaQueryWrapper<SupplierInfo>()
                .eq(status != null && !status.isBlank(), SupplierInfo::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SupplierInfo::getSupplierCode, keyword)
                        .or()
                        .like(SupplierInfo::getSupplierName, keyword))
                .orderByDesc(SupplierInfo::getUpdateTime);
        return Result.page(supplierInfoService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询供应商详情")
    @RequirePermission("masterdata:supplier:list")
    @GetMapping("/{id}")
    public Result<SupplierInfo> getById(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id) {
        return Result.success(supplierInfoService.getById(id));
    }

    @Operation(summary = "新增供应商")
    @RequirePermission("masterdata:supplier:add")
    @PostMapping
    public Result<SupplierInfo> save(@RequestBody SupplierInfo supplier) {
        return Result.success(supplierInfoService.createSupplier(supplier));
    }

    @Operation(summary = "更新供应商")
    @RequirePermission("masterdata:supplier:edit")
    @PutMapping
    public Result<SupplierInfo> update(@RequestBody SupplierInfo supplier) {
        return Result.success(supplierInfoService.updateSupplier(supplier));
    }

    @Operation(summary = "删除供应商")
    @RequirePermission("masterdata:supplier:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "供应商ID", required = true)
            @PathVariable Long id) {
        boolean ok = supplierInfoService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

package com.linan.plugin.masterdata.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.masterdata.entity.CustomerInfo;
import com.linan.plugin.masterdata.service.ICustomerInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户信息Controller
 *
 * @author BareZen Team
 */
@Tag(name = "主数据-客户", description = "客户信息管理接口")
@RestController
@RequestMapping("/api/masterdata/v1/customers")
@RequiredArgsConstructor
public class CustomerInfoController {

    private final ICustomerInfoService customerInfoService;

    @Operation(summary = "分页查询客户")
    @RequirePermission("masterdata:customer:list")
    @GetMapping("/page")
    public Result<PageResult<CustomerInfo>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "编码/名称模糊关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态: 0=正常, 1=停用") @RequestParam(required = false) String status) {
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<CustomerInfo>()
                .eq(status != null && !status.isBlank(), CustomerInfo::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(CustomerInfo::getCustomerCode, keyword)
                        .or()
                        .like(CustomerInfo::getCustomerName, keyword))
                .orderByDesc(CustomerInfo::getUpdateTime);
        return Result.page(customerInfoService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "查询客户详情")
    @RequirePermission("masterdata:customer:list")
    @GetMapping("/{id}")
    public Result<CustomerInfo> getById(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id) {
        return Result.success(customerInfoService.getById(id));
    }

    @Operation(summary = "新增客户")
    @RequirePermission("masterdata:customer:add")
    @PostMapping
    public Result<CustomerInfo> save(@RequestBody CustomerInfo customer) {
        return Result.success(customerInfoService.createCustomer(customer));
    }

    @Operation(summary = "更新客户")
    @RequirePermission("masterdata:customer:edit")
    @PutMapping
    public Result<CustomerInfo> update(@RequestBody CustomerInfo customer) {
        return Result.success(customerInfoService.updateCustomer(customer));
    }

    @Operation(summary = "删除客户")
    @RequirePermission("masterdata:customer:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id) {
        boolean ok = customerInfoService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

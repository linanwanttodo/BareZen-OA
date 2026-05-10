package com.linan.plugin.salary.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.salary.entity.SalaryStructure;
import com.linan.plugin.salary.service.ISalaryStructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工资结构Controller
 *
 * @author BareZen Team
 */
@Tag(name = "工资管理", description = "工资管理模块接口")
@RestController
@RequestMapping("/api/salary/v1/structures")
@RequiredArgsConstructor
public class SalaryStructureController {

    private final ISalaryStructureService salaryStructureService;

    @Operation(summary = "查询员工工资结构")
    @RequirePermission("salary:structure:list")
    @GetMapping("/user/{userId}")
    public Result<SalaryStructure> getByUserId(
            @Parameter(description = "员工ID", required = true)
            @PathVariable Long userId) {
        return Result.success(salaryStructureService.getByUserId(userId));
    }

    @Operation(summary = "计算员工实发工资")
    @RequirePermission("salary:structure:calculate")
    @GetMapping("/calculate/{userId}")
    public Result<BigDecimal> calculateActualSalary(
            @Parameter(description = "员工ID", required = true)
            @PathVariable Long userId) {
        return Result.success(salaryStructureService.calculateActualSalary(userId));
    }

    @Operation(summary = "批量计算实发工资")
    @RequirePermission("salary:structure:calculate")
    @PostMapping("/calculate/batch")
    public Result<List<SalaryStructure>> batchCalculate(@RequestBody List<Long> userIds) {
        return Result.success(salaryStructureService.batchCalculateActualSalary(userIds));
    }

    @Operation(summary = "新增工资结构")
    @RequirePermission("salary:structure:add")
    @PostMapping
    public Result<Void> save(@RequestBody SalaryStructure structure) {
        boolean ok = salaryStructureService.save(structure);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    @Operation(summary = "更新工资结构")
    @RequirePermission("salary:structure:edit")
    @PutMapping
    public Result<Void> update(@RequestBody SalaryStructure structure) {
        boolean ok = salaryStructureService.updateById(structure);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    @Operation(summary = "删除工资结构")
    @RequirePermission("salary:structure:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "工资结构ID", required = true)
            @PathVariable Long id) {
        boolean ok = salaryStructureService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}
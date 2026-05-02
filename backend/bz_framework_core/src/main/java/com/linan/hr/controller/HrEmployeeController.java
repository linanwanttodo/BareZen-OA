package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.hr.entity.HrEmployee;
import com.linan.hr.service.IHrEmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 员工档案 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "员工档案", description = "人事管理 - 员工档案 CRUD")
@RestController
@RequestMapping("/api/hr/v1/employees")
public class HrEmployeeController {

    @Autowired
    private IHrEmployeeService employeeService;

    /**
     * 条件分页查询
     */
    @RequirePermission("hr:employee:list")
    @Operation(summary = "分页查询员工档案")
    @GetMapping("/page")
    public Result<PageResult<HrEmployee>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String empNo,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status) {

        Page<HrEmployee> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<>();

        if (name != null && !name.isEmpty()) {
            wrapper.like(HrEmployee::getName, name);
        }
        if (empNo != null && !empNo.isEmpty()) {
            wrapper.like(HrEmployee::getEmpNo, empNo);
        }
        if (deptId != null) {
            wrapper.eq(HrEmployee::getDeptId, deptId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(HrEmployee::getStatus, status);
        }

        wrapper.orderByDesc(HrEmployee::getCreateTime);
        IPage<HrEmployee> result = employeeService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增员工
     */
    @Operation(summary = "新增员工")
    @OperLog(title = "员工档案", businessType = 1)
    @RequirePermission("hr:employee:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody HrEmployee employee) {
        boolean ok = employeeService.save(employee);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新员工
     */
    @Operation(summary = "修改员工")
    @OperLog(title = "员工档案", businessType = 2)
    @RequirePermission("hr:employee:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody HrEmployee employee) {
        boolean ok = employeeService.updateById(employee);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除员工
     */
    @Operation(summary = "删除员工")
    @OperLog(title = "员工档案", businessType = 3)
    @RequirePermission("hr:employee:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = employeeService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 获取员工详情
     */
    @RequirePermission("hr:employee:list")
    @Operation(summary = "查询员工详情")
    @GetMapping("/{id}")
    public Result<HrEmployee> getById(@PathVariable Long id) {
        HrEmployee employee = employeeService.getById(id);
        if (employee == null) {
            return Result.error("员工不存在");
        }
        return Result.success(employee);
    }
}

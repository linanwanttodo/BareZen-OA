package com.linan.web.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysDept;
import com.linan.system.service.ISysDeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 部门管理 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "部门管理", description = "部门树形结构与 CRUD")
@RestController
@RequestMapping("/api/system/v1/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final ISysDeptService deptService;

    /**
     * 获取部门树形列表
     * GET /api/system/v1/depts/tree
     */
    @RequirePermission("system:dept:list")
    @Operation(summary = "获取部门树形列表")
    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        List<SysDept> list = deptService.getDeptTree();
        return Result.success(list);
    }

    /**
     * 查询部门详情
     * GET /api/system/v1/depts/{id}
     */
    @RequirePermission("system:dept:list")
    @Operation(summary = "查询部门详情")
    @GetMapping("/{id}")
    public Result<SysDept> get(@PathVariable Long id) {
        SysDept dept = deptService.getById(id);
        return Result.success(dept);
    }

    /**
     * 新增部门
     * POST /api/system/v1/depts
     */
    @Operation(summary = "新增部门")
    @OperLog(title = "部门管理", businessType = 1)
    @RequirePermission("system:dept:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SysDept dept) {
        boolean ok = deptService.save(dept);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新部门
     */
    @Operation(summary = "更新部门")
    @OperLog(title = "部门管理", businessType = 2)
    @RequirePermission("system:dept:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysDept dept) {
        boolean ok = deptService.updateById(dept);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除部门
     * DELETE /api/system/v1/depts/{id}
     */
    @Operation(summary = "删除部门")
    @OperLog(title = "部门管理", businessType = 3)
    @RequirePermission("system:dept:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 先检查是否有子部门
        if (deptService.hasChildDept(id)) {
            return Result.error("存在子部门，无法删除");
        }
        boolean ok = deptService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 切换部门状态
     * PUT /api/system/v1/depts/{id}/status/{status}
     */
    @RequirePermission("system:dept:edit")
    @Operation(summary = "切换部门状态")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        deptService.updateDeptStatus(id, status);
        return Result.success(null);
    }
}

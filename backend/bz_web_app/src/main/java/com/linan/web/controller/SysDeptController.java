package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysDept;
import com.linan.system.mapper.SysDeptMapper;
import com.linan.system.service.ISysDeptService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理 Controller
 *
 * @author BareZen Team
 */
@RestController
@RequestMapping("/api/system/v1/depts")
public class SysDeptController {

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private SysDeptMapper deptMapper;

    /**
     * 获取部门树形列表
     * GET /api/system/v1/depts/tree
     */
    @RequirePermission("system:dept:list")
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
    @GetMapping("/{id}")
    public Result<SysDept> get(@PathVariable Long id) {
        SysDept dept = deptService.getById(id);
        return Result.success(dept);
    }

    /**
     * 新增部门
     * POST /api/system/v1/depts
     */
    @RequirePermission("system:dept:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SysDept dept) {
        boolean ok = deptService.save(dept);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新部门
     */
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
    @RequirePermission("system:dept:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 先检查是否有子部门
        Long childCount = deptMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getParentId, id)
                        .eq(SysDept::getDelFlag, 0));
        if (childCount > 0) {
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
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setStatus(status);
        boolean ok = deptService.updateById(dept);
        return ok ? Result.success(null) : Result.error("状态切换失败");
    }
}

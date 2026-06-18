package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysRole;
import com.linan.system.service.ISysRoleService;
import com.linan.core.common.api.PageParamGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 角色管理 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "角色管理", description = "角色 CRUD 与权限分配")
@RestController
@RequestMapping("/api/system/v1/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService roleService;
    /**
     * 条件分页查询
     * GET /api/system/v1/roles/page?pageNum=1&pageSize=10&roleName=admin&status=1
     */
    @RequirePermission("system:role:list")
    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public Result<PageResult<SysRole>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) String status) {

        // 分页参数钳制（防全表扫描）
        PageParamGuard.validate(pageNum, pageSize);

        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        if (roleName != null && !roleName.isEmpty()) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        if (roleKey != null && !roleKey.isEmpty()) {
            wrapper.like(SysRole::getRoleKey, roleKey);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysRole::getStatus, status);
        }

        wrapper.orderByAsc(SysRole::getRoleSort);
        IPage<SysRole> result = roleService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增角色
     */
    @Operation(summary = "新增角色")
    @OperLog(title = "角色管理", businessType = 1)
    @RequirePermission("system:role:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SysRole role) {
        // 检查角色标识唯一
        if (!roleService.checkRoleKeyUnique(role.getRoleKey())) {
            return Result.error("角色标识已存在");
        }
        boolean ok = roleService.save(role);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新角色
     */
    @Operation(summary = "更新角色")
    @OperLog(title = "角色管理", businessType = 2)
    @RequirePermission("system:role:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysRole role) {
        // 更新前复查 roleKey 唯一（排除自身）：save 用的 checkRoleKeyUnique 不支持
        // 排除自身 id，这里按相同语义（存在即冲突）用 lambdaQuery 复查
        if (role.getId() != null && role.getRoleKey() != null) {
            long conflict = roleService.lambdaQuery()
                    .eq(SysRole::getRoleKey, role.getRoleKey())
                    .ne(SysRole::getId, role.getId())
                    .count();
            if (conflict > 0) {
                return Result.error("角色标识已存在");
            }
        }
        boolean ok = roleService.updateById(role);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除角色
     */
    @Operation(summary = "删除角色")
    @OperLog(title = "角色管理", businessType = 3)
    @RequirePermission("system:role:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = roleService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 批量删除
     */
    @Operation(summary = "批量删除角色")
    @OperLog(title = "角色管理", businessType = 3)
    @RequirePermission("system:role:delete")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        boolean ok = roleService.removeByIds(ids);
        return ok ? Result.success(null) : Result.error("批量删除失败");
    }

    /**
     * 给角色分配菜单权限
     * PUT /api/system/v1/roles/{roleId}/menus
     */
    @Operation(summary = "分配菜单权限")
    @OperLog(title = "角色管理", businessType = 2)
    @RequirePermission("system:role:edit")
    @PutMapping("/{roleId}/menus")
    public Result<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignRoleMenus(roleId, menuIds);
        return Result.success(null);
    }

    /**
     * 查询角色已分配的菜单ID列表（分配弹窗回显）
     * GET /api/system/v1/roles/{roleId}/menus
     */
    @Operation(summary = "查询角色菜单ID列表")
    @RequirePermission("system:role:list")
    @GetMapping("/{roleId}/menus")
    public Result<List<Long>> getRoleMenuIds(@PathVariable Long roleId) {
        return Result.success(roleService.getMenuIdsByRoleId(roleId));
    }

    /**
     * 切换角色状态
     */
    @Operation(summary = "切换角色状态")
    @RequirePermission("system:role:edit")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable String status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setStatus(status);
        boolean ok = roleService.updateById(role);
        return ok ? Result.success(null) : Result.error("状态切换失败");
    }
}
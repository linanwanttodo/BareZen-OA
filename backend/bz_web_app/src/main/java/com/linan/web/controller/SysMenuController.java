package com.linan.web.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysMenu;
import com.linan.system.service.ISysMenuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 菜单管理 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "菜单管理", description = "菜单树、CRUD 与权限标识管理")
@RestController
@RequestMapping("/api/system/v1/menus")
public class SysMenuController {

    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取当前用户的路由菜单（用于前端动态路由）
     * GET /api/system/v1/menus/routers
     */
    @Operation(summary = "获取当前用户路由菜单")
    @GetMapping("/routers")
    public Result<List<SysMenu>> getRouters() {
        Long userId = AuthInterceptor.getCurrentUserId();
        List<SysMenu> tree = menuService.selectMenuTree(userId);
        return Result.success(tree);
    }

    /**
     * 获取完整菜单树（用于菜单管理页面）
     * GET /api/system/v1/menus/treeselect
     */
    @RequirePermission("system:menu:list")
    @Operation(summary = "获取完整菜单树")
    @GetMapping("/treeselect")
    public Result<List<SysMenu>> treeselect() {
        List<SysMenu> tree = menuService.selectMenuTreeList();
        return Result.success(tree);
    }

    /**
     * 分页查询菜单列表
     */
    @RequirePermission("system:menu:list")
    @Operation(summary = "分页查询菜单列表")
    @GetMapping("/list")
    public Result<List<SysMenu>> list() {
        List<SysMenu> list = menuService.selectMenuList();
        return Result.success(list);
    }

    /**
     * 新增菜单
     */
    @Operation(summary = "新增菜单")
    @OperLog(title = "菜单管理", businessType = 1)
    @RequirePermission("system:menu:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SysMenu menu) {
        boolean ok = menuService.save(menu);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新菜单
     */
    @Operation(summary = "更新菜单")
    @OperLog(title = "菜单管理", businessType = 2)
    @RequirePermission("system:menu:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysMenu menu) {
        boolean ok = menuService.updateById(menu);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除菜单
     */
    @Operation(summary = "删除菜单")
    @OperLog(title = "菜单管理", businessType = 3)
    @RequirePermission("system:menu:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有子菜单
        if (menuService.countChildMenus(id) > 0) {
            return Result.error("请先删除子菜单");
        }
        // 检查是否有角色引用此菜单
        if (menuService.countRoleMenu(id) > 0) {
            return Result.error("该菜单已被角色引用，无法删除");
        }
        boolean ok = menuService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}
package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.mapper.SysRoleMenuMapper;
import com.linan.system.entity.SysMenu;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.service.ISysMenuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 菜单管理 Controller
 *
 * @author BareZen Team
 */
@RestController
@RequestMapping("/api/system/v1/menus")
public class SysMenuController {

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    /**
     * 获取当前用户的路由菜单（用于前端动态路由）
     * GET /api/system/v1/menus/routers
     */
    @GetMapping("/routers")
    public Result<List<SysMenu>> getRouters() {
        Long userId = AuthInterceptor.getCurrentUserId();
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        List<SysMenu> tree = buildTree(menus);
        return Result.success(tree);
    }

    /**
     * 获取完整菜单树（用于菜单管理页面）
     * GET /api/system/v1/menus/treeselect
     */
    @RequirePermission("system:menu:list")
    @GetMapping("/treeselect")
    public Result<List<SysMenu>> treeselect() {
        List<SysMenu> all = menuService.lambdaQuery()
                .in(SysMenu::getMenuType, "M", "C")
                .eq(SysMenu::getDelFlag, 0)
                .orderByAsc(SysMenu::getOrderNum)
                .list();
        List<SysMenu> tree = buildTree(all);
        return Result.success(tree);
    }

    /**
     * 分页查询菜单列表
     */
    @RequirePermission("system:menu:list")
    @GetMapping("/list")
    public Result<List<SysMenu>> list() {
        List<SysMenu> list = menuService.lambdaQuery()
                .eq(SysMenu::getDelFlag, 0)
                .orderByAsc(SysMenu::getOrderNum)
                .list();
        return Result.success(list);
    }

    /**
     * 新增菜单
     */
    @RequirePermission("system:menu:add")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SysMenu menu) {
        boolean ok = menuService.save(menu);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新菜单
     */
    @RequirePermission("system:menu:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysMenu menu) {
        boolean ok = menuService.updateById(menu);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除菜单
     */
    @RequirePermission("system:menu:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有子菜单
        Long childCount = menuService.lambdaQuery()
                .eq(SysMenu::getParentId, id)
                .eq(SysMenu::getDelFlag, 0)
                .count();
        if (childCount > 0) {
            return Result.error("请先删除子菜单");
        }
        // 检查是否有角色引用此菜单
        Long roleCount = roleMenuMapper.countByMenuId(id);
        if (roleCount > 0) {
            return Result.error("该菜单已被角色引用，无法删除");
        }
        boolean ok = menuService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildTree(List<SysMenu> list) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : list) {
            if (menu.getParentId() == 0) {
                menu.setChildren(getChildren(menu.getId(), list));
                tree.add(menu);
            }
        }
        tree.sort(Comparator.comparingInt(m -> m.getOrderNum() == null ? 0 : m.getOrderNum()));
        return tree;
    }

    private List<SysMenu> getChildren(Long parentId, List<SysMenu> all) {
        List<SysMenu> children = new ArrayList<>();
        for (SysMenu menu : all) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(getChildren(menu.getId(), all));
                children.add(menu);
            }
        }
        children.sort(Comparator.comparingInt(m -> m.getOrderNum() == null ? 0 : m.getOrderNum()));
        return children;
    }
}
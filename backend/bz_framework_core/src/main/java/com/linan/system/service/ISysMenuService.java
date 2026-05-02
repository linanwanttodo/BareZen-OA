package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysMenu;

import java.util.List;

public interface ISysMenuService extends IService<SysMenu> {

    /**
     * 根据用户ID查询菜单树（用于前端动态路由）
     */
    List<SysMenu> selectMenuTree(Long userId);

    /**
     * 查询所有未删除的菜单列表（用于菜单管理列表）
     */
    List<SysMenu> selectMenuList();

    /**
     * 查询目录和菜单类型的菜单树（用于菜单管理树形选择）
     */
    List<SysMenu> selectMenuTreeList();

    /**
     * 统计指定父菜单下的子菜单数量
     */
    long countChildMenus(Long parentId);

    /**
     * 统计指定菜单被角色引用的次数
     */
    long countRoleMenu(Long menuId);
}
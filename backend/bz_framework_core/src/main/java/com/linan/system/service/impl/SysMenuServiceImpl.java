package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysMenu;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMenuMapper;
import com.linan.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<SysMenu> selectMenuTree(Long userId) {
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        return buildTree(menus);
    }

    @Override
    public List<SysMenu> selectMenuList() {
        return lambdaQuery()
                .eq(SysMenu::getDelFlag, 0)
                .orderByAsc(SysMenu::getOrderNum)
                .list();
    }

    @Override
    public List<SysMenu> selectMenuTreeList() {
        List<SysMenu> list = lambdaQuery()
                .in(SysMenu::getMenuType, "M", "C")
                .eq(SysMenu::getDelFlag, 0)
                .orderByAsc(SysMenu::getOrderNum)
                .list();
        return buildTree(list);
    }

    @Override
    public long countChildMenus(Long parentId) {
        return lambdaQuery()
                .eq(SysMenu::getParentId, parentId)
                .eq(SysMenu::getDelFlag, 0)
                .count();
    }

    @Override
    public long countRoleMenu(Long menuId) {
        return roleMenuMapper.countByMenuId(menuId);
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
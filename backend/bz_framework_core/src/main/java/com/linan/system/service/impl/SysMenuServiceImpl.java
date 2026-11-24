package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysMenu;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMenuMapper;
import com.linan.system.service.ISysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    private final SysRoleMenuMapper roleMenuMapper;

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
     * 构建菜单树（单次遍历 O(n log n)，用 HashMap 按 parentId 分组替代递归全表扫描）
     */
    private List<SysMenu> buildTree(List<SysMenu> list) {
        Map<Long, List<SysMenu>> childrenByParent = new LinkedHashMap<>();
        for (SysMenu menu : list) {
            childrenByParent.computeIfAbsent(menu.getParentId(), k -> new ArrayList<>()).add(menu);
        }
        Comparator<SysMenu> byOrder = Comparator.comparingInt(m -> m.getOrderNum() == null ? 0 : m.getOrderNum());
        childrenByParent.values().forEach(group -> group.sort(byOrder));

        List<SysMenu> roots = childrenByParent.getOrDefault(0L, List.of());
        for (SysMenu root : roots) {
            attachChildren(root, childrenByParent);
        }
        return roots;
    }

    private void attachChildren(SysMenu parent, Map<Long, List<SysMenu>> childrenByParent) {
        List<SysMenu> children = childrenByParent.getOrDefault(parent.getId(), List.of());
        parent.setChildren(children);
        for (SysMenu child : children) {
            attachChildren(child, childrenByParent);
        }
    }
}
package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.SysRole;
import com.linan.system.entity.SysRoleMenu;
import com.linan.system.mapper.SysRoleMapper;
import com.linan.system.mapper.SysRoleMenuMapper;
import com.linan.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        // 1. 删除该角色原有的权限关联
        roleMenuMapper.deleteByRoleId(roleId);

        // 2. 批量插入新的权限关联
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> list = menuIds.stream().map(menuId -> {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                return rm;
            }).collect(Collectors.toList());
            roleMenuMapper.batchInsert(list);
        }
    }

    @Override
    public boolean checkRoleKeyUnique(String roleKey) {
        long count = baseMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleKey, roleKey));
        return count == 0;
    }
}
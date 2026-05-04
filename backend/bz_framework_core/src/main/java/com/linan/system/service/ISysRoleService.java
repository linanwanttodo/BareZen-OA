package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysRole;

import java.util.List;

public interface ISysRoleService extends IService<SysRole> {

    /**
     * 给角色分配菜单权限
     */
    void assignRoleMenus(Long roleId, List<Long> menuIds);

    /**
     * 查询角色已分配的菜单ID列表（用于分配弹窗回显）
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 检查角色标识是否唯一
     *
     * @return true 表示唯一（不存在），false 表示已存在
     */
    boolean checkRoleKeyUnique(String roleKey);
}
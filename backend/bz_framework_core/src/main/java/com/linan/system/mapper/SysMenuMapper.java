package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT m.perms FROM sys_menu m " +
            "LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} " +
            "AND m.perms IS NOT NULL AND m.perms != '' " +
            "AND m.del_flag = 0")
    List<String> selectPermsByRoleId(@Param("roleId") Long roleId);

    /**
     * 按用户一次取回全部权限字符串，替代「查角色后逐角色查权限」的 N+1。
     * 过滤条件与 selectPermsByRoleId 严格一致：不限 menu_type（F 型按钮权限必须包含在内），
     * 否则收敛成单条查询时按钮级权限会静默丢失。
     */
    @Select("SELECT DISTINCT m.perms FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.perms IS NOT NULL AND m.perms != '' " +
            "AND m.del_flag = 0")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "LEFT JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.menu_type IN ('M', 'C') " +
            "AND m.status = '0' " +
            "AND m.del_flag = 0 " +
            "ORDER BY m.order_num ASC")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}

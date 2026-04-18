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

    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "LEFT JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.menu_type IN ('M', 'C') " +
            "AND m.status = '1' " +
            "AND m.del_flag = 0 " +
            "ORDER BY m.order_num ASC")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}

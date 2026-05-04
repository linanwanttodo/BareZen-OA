package com.linan.system.mapper;

import com.linan.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色菜单关联 Mapper
 */
@Mapper
public interface SysRoleMenuMapper {

    /**
     * 统计指定菜单被多少个角色引用
     */
    @Select("SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = #{menuId}")
    Long countByMenuId(@Param("menuId") Long menuId);

    /**
     * 查询角色已分配的全部菜单ID
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID删除关联
     */
    void deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入关联
     */
    void batchInsert(@Param("list") List<SysRoleMenu> list);
}

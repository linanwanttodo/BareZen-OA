package com.linan.system.mapper;

import com.linan.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 根据用户ID查询角色关联列表
     */
    @Select("SELECT * FROM sys_user_role WHERE user_id = #{userId}")
    List<SysUserRole> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除所有角色关联
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 插入一条用户-角色关联
     */
    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insert(SysUserRole record);

    /**
     * 批量插入用户-角色关联（一次 SQL，替代循环逐条 insert）
     */
    @Insert("<script>INSERT INTO sys_user_role(user_id, role_id) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.userId}, #{item.roleId})"
            + "</foreach></script>")
    int insertBatch(@Param("list") List<SysUserRole> list);
}

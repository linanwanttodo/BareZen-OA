package com.linan.system.mapper;

import com.linan.system.entity.SysUserDept;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-部门关联 Mapper
 *
 * @author BareZen Team
 */
@Mapper
public interface SysUserDeptMapper {

    /**
     * 插入一条用户-部门关联
     */
    @Insert("INSERT INTO sys_user_dept(user_id, dept_id, role_type) VALUES(#{userId}, #{deptId}, #{roleType})")
    int insert(SysUserDept record);

    /**
     * 批量插入用户-部门关联（一次 SQL，替代循环逐条 insert）
     */
    @Insert("<script>INSERT INTO sys_user_dept(user_id, dept_id, role_type) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.userId}, #{item.deptId}, #{item.roleType})"
            + "</foreach></script>")
    int insertBatch(@Param("list") List<SysUserDept> list);

    /**
     * 根据用户ID查询该用户所属部门关联
     */
    @Select("SELECT * FROM sys_user_dept WHERE user_id = #{userId}")
    List<SysUserDept> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询其所属的所有部门ID
     */
    @Select("SELECT dept_id FROM sys_user_dept WHERE user_id = #{userId}")
    List<Long> selectDeptIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除所有部门关联
     */
    void deleteByUserId(@Param("userId") Long userId);
}

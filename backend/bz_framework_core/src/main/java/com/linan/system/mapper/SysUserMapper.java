package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.system.entity.SysUser;
import com.linan.system.entity.UserRoleDeptVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper 接口
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按角色ID和/或部门ID筛选用户（带 MyBatis-Plus 分页）
     * @param page     分页参数
     * @param roleIds  角色ID列表（可为空）
     * @param deptIds  部门ID列表（可为空）
     * @param userName 用户名模糊搜索（可为空）
     * @param status   用户状态（可为空）
     * @return 分页后的用户列表
     */
    Page<SysUser> selectUsersByRoleDeptPage(
            Page<?> page,
            @Param("roleIds") List<Long> roleIds,
            @Param("deptIds") List<Long> deptIds,
            @Param("userName") String userName,
            @Param("status") String status);

    /**
     * 批量查询所有用户的角色名称
     */
    List<UserRoleDeptVo> selectUserRoleNames();

    /**
     * 批量查询所有用户的部门名称
     */
    List<UserRoleDeptVo> selectUserDeptNames();
}
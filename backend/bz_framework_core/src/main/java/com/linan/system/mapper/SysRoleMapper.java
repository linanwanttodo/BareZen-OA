package com.linan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper 接口
 *
 * 继承 BaseMapper<SysRole> 自动获得 CRUD 方法。
 * 额外提供根据用户ID查询角色列表的自定义方法。
 *
 * @author BareZen Team
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询该用户拥有的所有角色
     *
     * 联表查询：sys_user_role + sys_role
     * 两表通过 user_id 关联，返回角色完整信息。
     *
     * @param userId 用户ID
     * @return 角色列表（可能为空列表）
     */
    @Select("SELECT r.* FROM sys_role r " +
            "LEFT JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND r.del_flag = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}

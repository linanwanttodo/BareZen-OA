package com.linan.system.entity;

import lombok.Data;

/**
 * 用户-角色-部门 关联查询 VO
 * 用于批量查询用户所属的角色名称和部门名称
 */
@Data
public class UserRoleDeptVo {
    /** 用户ID */
    private Long userId;

    /** 角色名称（逗号分隔） */
    private String roleNames;

    /** 部门名称（逗号分隔） */
    private String deptNames;
}

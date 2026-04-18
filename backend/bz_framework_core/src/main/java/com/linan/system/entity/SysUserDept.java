package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-部门关联实体（含部门内角色类型）
 *
 * @author BareZen Team
 */
@Data
@TableName("sys_user_dept")
public class SysUserDept {

    /** 用户ID */
    private Long userId;

    /** 部门ID */
    private Long deptId;

    /** 部门内角色（0=普通用户 1=管理员） */
    private String roleType;
}

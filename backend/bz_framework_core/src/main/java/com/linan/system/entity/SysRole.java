package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体类，对应 sys_role 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    private Integer roleSort;

    @NotBlank(message = "状态不能为空")
    private String status;

    /** 数据权限范围：1=仅本人 2=本部门 3=本部门及子部门 4=全部 */
    private Integer dataScope;

    private String remark;
}
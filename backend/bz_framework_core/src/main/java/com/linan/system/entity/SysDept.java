package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体类
 * 对应数据库表 sys_dept，树形层级结构
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 父部门ID（0=顶级部门） */
    private Long parentId;

    /** 部门名称（必填） */
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    /** 显示顺序 */
    private Integer sort;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 部门状态（0=正常 1=停用） */
    @NotBlank(message = "状态不能为空")
    private String status;
}

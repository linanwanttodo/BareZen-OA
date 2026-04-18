package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单权限实体类，对应 sys_menu 表
 * menu_type: M=目录 C=菜单 F=按钮
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    private String perms;

    @NotBlank(message = "路由地址不能为空")
    private String path;

    private String component;
    private String icon;
    private Integer orderNum;
    private String visible;
    private String status;
    private String remark;

    /** 子菜单列表（非数据库字段，仅用于树形结构返回） */
    @TableField(exist = false)
    private List<SysMenu> children;
}

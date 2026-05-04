package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型实体
 * 对应数据表 sys_dict_type
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {
    /** 字典名称（如：用户性别） */
    private String dictName;
    /** 字典类型编码（如：sys_user_sex，全局唯一） */
    private String dictType;
    /** 状态（0=停用 1=正常，与 sys_dict_type 表注释一致） */
    private String status;
}
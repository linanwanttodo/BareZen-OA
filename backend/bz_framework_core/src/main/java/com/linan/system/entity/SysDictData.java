package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据实体
 * 对应数据表 sys_dict_data
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {
    /**
     * 字典编码（如：1）
     * 注意：sys_dict_data 表并无 dict_code 列（表结构以 V2 迁移为准），
     * 该字段仅作前端展示占位，标记为不存在于表，避免 MyBatis-Plus 生成对
     * dict_code 列的查询（会导致 Unknown column 报错）。
     */
    @TableField(exist = false)
    private String dictCode;
    /** 字典标签（如：男） */
    private String dictLabel;
    /** 字典值（如：0） */
    private String dictValue;
    /** 所属字典类型编码（关联 sys_dict_type.dict_type） */
    private String dictType;
    /** 字典排序 */
    private Integer dictSort;
    /** 样式属性（CSS 类名，如：primary） */
    private String cssClass;
    /** 表格回显样式（如：success） */
    private String listClass;
    /** 是否默认（Y/N） */
    private String isDefault;

    /** 状态（0=停用 1=正常，与 sys_dict_data 表注释一致） */
    private String status;
}
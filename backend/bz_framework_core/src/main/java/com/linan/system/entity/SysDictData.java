package com.linan.system.entity;

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
    /** 字典编码（如：1） */
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
    /** 状态（0正常 1停用） */
    private String status;
}
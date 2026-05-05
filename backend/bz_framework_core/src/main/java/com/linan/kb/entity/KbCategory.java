package com.linan.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分类实体，对应 kb_category 表
 *
 * <p>V41 新增：替代 V13 硬编码枚举分类（general/manual/process/tech/other），
 * 支持树形扩展（parent_id=0 为根分类）。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_category")
public class KbCategory extends BaseEntity {

    /** 分类名称 */
    private String name;

    /** 父分类ID（0=根分类） */
    private Long parentId;

    /** 排序号 */
    private Integer sort;

    /** 是否启用：1=启用 0=停用 */
    private Integer status;
}
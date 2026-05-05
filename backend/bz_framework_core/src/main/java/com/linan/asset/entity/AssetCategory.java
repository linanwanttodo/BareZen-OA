package com.linan.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产分类实体，对应 asset_category 表
 *
 * <p>V39 新增：替代 V11 硬编码枚举分类（computer/equipment/furniture/vehicle/other），
 * 支持树形扩展（parent_id=0 为根分类）。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_category")
public class AssetCategory extends BaseEntity {

    /** 分类名称 */
    private String name;

    /** 父分类ID（0=根分类） */
    private Long parentId;

    /** 排序号 */
    private Integer sort;

    /** 是否启用：1=启用 0=停用 */
    private Integer status;
}
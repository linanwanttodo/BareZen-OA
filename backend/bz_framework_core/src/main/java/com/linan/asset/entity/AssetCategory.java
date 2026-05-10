package com.linan.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "资产分类实体")
public class AssetCategory extends BaseEntity {

    @Schema(description = "分类名称", example = "办公设备", required = true)
    private String name;

    @Schema(description = "父分类ID（0=根分类）", example = "0")
    private Long parentId;

    @Schema(description = "排序号", example = "1")
    private Integer sort;

    @Schema(description = "是否启用：1=启用 0=停用", example = "1", allowableValues = {"0", "1"})
    private Integer status;
}
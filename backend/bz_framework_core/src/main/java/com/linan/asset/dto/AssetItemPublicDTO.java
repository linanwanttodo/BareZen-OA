package com.linan.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资产公开视图 DTO（用户前台可借用资产展示）
 *
 * <p>仅暴露员工借用场景所需字段；购置价格、购置日期、使用部门、使用人、
 * 报废日期、备注等内部管理字段不对外返回。</p>
 *
 * @author BareZen Team
 */
@Data
@Schema(description = "资产公开视图（用户前台）")
public class AssetItemPublicDTO {

    @Schema(description = "资产ID", example = "1")
    private Long id;

    @Schema(description = "资产编号", example = "ZC-2024-001")
    private String assetNo;

    @Schema(description = "资产名称", example = "ThinkPad X1 笔记本")
    private String assetName;

    @Schema(description = "资产分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "资产分类枚举（兼容旧口径）", example = "computer")
    private String category;

    @Schema(description = "品牌", example = "联想")
    private String brand;

    @Schema(description = "型号", example = "X1 Carbon")
    private String model;

    @Schema(description = "存放位置", example = "A栋3层IT库房")
    private String location;

    @Schema(description = "状态：0=在库（用户端仅返回该状态）", example = "0")
    private String status;
}

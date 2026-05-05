package com.linan.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 资产台账实体，对应 asset_item 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_item")
public class AssetItem extends BaseEntity {

    /** 资产编号 */
    @NotBlank(message = "资产编号不能为空")
    private String assetNo;

    /** 资产名称 */
    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    /** 资产分类：computer=电脑办公 equipment=设备 furniture=家具 vehicle=车辆 other=其他 */
    private String category;

    /** 资产分类ID（V39：关联 asset_category，替代枚举字符串） */
    private Long categoryId;

    /** 品牌 */
    private String brand;

    /** 型号 */
    private String model;

    /** 购置价格（元） */
    private BigDecimal price;

    /** 购置日期 */
    private String purchaseDate;

    /** 使用部门 */
    private String deptName;

    /** 使用人 */
    private String holderName;

    /** 存放位置 */
    private String location;

    /** 状态：0=在库 1=使用中 2=维修中 3=已报废 */
    private String status;

    /** 报废日期 */
    private String scrapDate;

    /** 备注 */
    private String remark;
}

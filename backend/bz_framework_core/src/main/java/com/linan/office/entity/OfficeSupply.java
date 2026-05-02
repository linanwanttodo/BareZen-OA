package com.linan.office.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 办公用品实体，对应 office_supply 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_supply")
public class OfficeSupply extends BaseEntity {

    /** 用品名称 */
    @NotBlank(message = "用品名称不能为空")
    private String supplyName;

    /** 分类：stationery=文具 electronics=电子耗材 cleaning=清洁用品 other=其他 */
    private String category;

    /** 规格型号 */
    private String spec;

    /** 库存数量 */
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /** 单位 */
    private String unit;

    /** 状态：0=正常 1=停用 */
    private String status;

    /** 备注 */
    private String remark;
}

package com.linan.plugin.masterdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物料信息实体
 *
 * @author BareZen Team
 */
@Data
@TableName("material_info")
@Schema(description = "物料信息")
public class MaterialInfo {

    @TableId(type = IdType.AUTO)
    @Schema(description = "物料ID")
    private Long id;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "状态: 0=正常, 1=停用")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "更新人")
    private Long updateBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 主数据为物理删除（V58 起契约，风格对齐 salary V56），del_flag 仅作历史列保留。
     * 不标 @TableLogic：标了会把 removeById 变成软删，uk_material_code 永久占用，
     * 删除后无法重建同编码物料。
     */
    @Schema(description = "删除标记")
    @JsonIgnore
    private Integer delFlag;
}

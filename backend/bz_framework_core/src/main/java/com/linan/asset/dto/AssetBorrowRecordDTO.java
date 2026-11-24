package com.linan.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资产借用记录数据传输对象
 *
 * @author BareZen Team
 */
@Data
@Schema(description = "资产借用记录数据传输对象")
public class AssetBorrowRecordDTO {

    @Schema(description = "借用记录ID", example = "1")
    private Long id;

    @Schema(description = "资产ID", example = "1", required = true)
    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    @Schema(description = "借用人用户ID", example = "100", required = true)
    @NotNull(message = "借用人ID不能为空")
    private Long borrowerId;

    @Schema(description = "借用人姓名", example = "张三", required = true)
    private String borrowerName;

    @Schema(description = "借用时间（yyyy-MM-dd HH:mm:ss）", example = "2024-01-01 09:00:00")
    private String borrowTime;

    @Schema(description = "预计归还时间", example = "2024-01-07 18:00:00")
    private String expectReturnTime;

    @Schema(description = "实际归还时间", example = "2024-01-07 17:30:00")
    private String returnTime;

    @Schema(description = "借用备注", example = "项目使用")
    private String remark;

    @Schema(description = "状态：0=借用中 1=已归还", example = "0", allowableValues = {"0", "1"})
    private String status;

    @Schema(description = "资产编号（列表展示用冗余字段，可空）", example = "ZC-2024-001")
    private String assetNo;

    @Schema(description = "资产名称（列表展示用冗余字段，可空）", example = "ThinkPad X1 笔记本")
    private String assetName;
}
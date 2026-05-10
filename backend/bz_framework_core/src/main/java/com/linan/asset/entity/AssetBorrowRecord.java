package com.linan.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产借用记录实体，对应 asset_borrow_record 表
 *
 * <p>V39 新增：轻量借用/归还流程（不走审批引擎），记录借用人与归还时间。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_borrow_record")
@Schema(description = "资产借用记录实体")
public class AssetBorrowRecord extends BaseEntity {

    @Schema(description = "资产ID", example = "1", required = true)
    private Long assetId;

    @Schema(description = "借用人用户ID", example = "100", required = true)
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
}
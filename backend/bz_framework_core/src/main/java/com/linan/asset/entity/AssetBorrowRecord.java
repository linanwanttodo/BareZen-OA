package com.linan.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
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
public class AssetBorrowRecord extends BaseEntity {

    /** 资产ID */
    private Long assetId;

    /** 借用人用户ID */
    private Long borrowerId;

    /** 借用人姓名 */
    private String borrowerName;

    /** 借用时间（yyyy-MM-dd HH:mm:ss） */
    private String borrowTime;

    /** 预计归还时间 */
    private String expectReturnTime;

    /** 实际归还时间 */
    private String returnTime;

    /** 借用备注 */
    private String remark;

    /** 状态：0=借用中 1=已归还 */
    private String status;
}
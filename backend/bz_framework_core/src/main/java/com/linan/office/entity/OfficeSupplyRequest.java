package com.linan.office.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用品申领记录实体，对应 office_supply_request 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_supply_request")
public class OfficeSupplyRequest extends BaseEntity {

    /** 用品ID */
    @NotNull(message = "请选择用品")
    private Long supplyId;

    /** 用品名称（冗余） */
    private String supplyName;

    /** 申领人用户ID */
    private Long userId;

    /** 申领人姓名（冗余） */
    private String userName;

    /** 申领人部门（冗余） */
    private String deptName;

    /** 申领数量 */
    @Min(value = 1, message = "申领数量至少为 1")
    private Integer quantity;

    /** 申领事由 */
    private String reason;

    /** 状态：0=待审批 1=已通过 2=已驳回 3=已取消 */
    private String status;

    /** 审批人用户ID */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批时间 */
    private String approveTime;

    /** 审批意见 */
    private String approveRemark;
}

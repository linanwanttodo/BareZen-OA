package com.linan.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 出差申请实体，对应 att_travel_request 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_travel_request")
public class AttTravelRequest extends BaseEntity {

    /** 申请人用户ID */
    private Long userId;

    /** 申请人姓名（冗余） */
    private String userName;

    /** 申请人部门（冗余） */
    private String deptName;

    /** 目的地 */
    private String destination;

    /** 出差事由 */
    private String purpose;

    /** 开始时间（yyyy-MM-dd HH:mm） */
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 结束时间（yyyy-MM-dd HH:mm） */
    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    /** 出差天数 */
    private BigDecimal days;

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

    /** 指定审批人用户ID（NULL=任意有权限者审批，V31 新增） */
    private Long assigneeId;

    /** 指定审批人姓名 */
    private String assigneeName;
}

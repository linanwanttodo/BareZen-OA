package com.linan.office.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用车申请实体，对应 office_vehicle_request 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_vehicle_request")
public class OfficeVehicleRequest extends BaseEntity {

    /** 申请人用户ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请部门 */
    private String deptName;

    /** 车型：car=轿车 van=商务车 bus=客车 other=其他 */
    private String vehicleType;

    /** 出发地（V54 新增） */
    private String departure;

    /** 用车事由 */
    @NotBlank(message = "用车事由不能为空")
    private String purpose;

    /** 用车开始时间 */
    @NotBlank(message = "用车开始时间不能为空")
    private String startTime;

    /** 用车结束时间 */
    @NotBlank(message = "用车结束时间不能为空")
    private String endTime;

    /** 目的地 */
    private String destination;

    /** 乘车人数 */
    private Integer passengerCount;

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

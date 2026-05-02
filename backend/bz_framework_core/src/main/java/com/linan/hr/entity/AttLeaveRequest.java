package com.linan.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 请假申请实体，对应 att_leave_request 表
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_leave_request")
public class AttLeaveRequest extends BaseEntity {

    /** 申请人用户ID */
    private Long userId;

    /** 申请人姓名（冗余） */
    private String userName;

    /** 申请人部门（冗余） */
    private String deptName;

    /** 请假类型：annual=年假 sick=病假 personal=事假 overtime=调休 marriage=婚假 maternity=产假 other=其他 */
    private String leaveType;

    /** 开始时间（yyyy-MM-dd HH:mm） */
    private String startTime;

    /** 结束时间（yyyy-MM-dd HH:mm） */
    private String endTime;

    /** 请假天数 */
    private BigDecimal leaveDays;

    /** 请假事由 */
    private String reason;

    /** 状态：0=待审批 1=已通过 2=已驳回 3=已撤销 */
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

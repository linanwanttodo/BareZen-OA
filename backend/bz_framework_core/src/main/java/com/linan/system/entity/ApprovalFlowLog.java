package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批流程历史实体，对应 approval_flow_log 表
 *
 * <p>记录各业务申请单的流程节点（提交/审批/撤销），供前端展示办事进程图。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow_log")
public class ApprovalFlowLog extends BaseEntity {

    /** 业务类型：leave=请假 overtime=加班 travel=出差 vehicle=用车 supply=用品申领 */
    private String bizType;

    /** 业务记录ID */
    private Long bizId;

    /** 节点名称：提交申请/审批通过/审批驳回/撤销申请 */
    private String nodeName;

    /** 节点后状态：0=待审批 1=已通过 2=已驳回 3=已取消 */
    private String nodeStatus;

    /** 操作人用户ID（提交人/审批人） */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 备注（审批意见等） */
    private String remark;

    /** 节点时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

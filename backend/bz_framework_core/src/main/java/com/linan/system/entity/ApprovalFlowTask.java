package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批节点任务实体，对应 approval_flow_task 表
 *
 * <p>每行 = 某单据的一个审批节点；同一单据同一时刻至多一行 status='0'
 * （当前待审节点）。审批通过后按模板插入后续节点，末节点通过/任意节点驳回即终态。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow_task")
public class ApprovalFlowTask extends BaseEntity {

    /** 业务类型：leave=请假 overtime=加班 travel=出差 vehicle=用车 */
    private String bizType;

    /** 业务记录ID */
    private Long bizId;

    /** 节点顺序（对应模板） */
    private Integer nodeSeq;

    /** 节点名称 */
    private String nodeName;

    /** 节点状态：0=待审批 1=已通过 2=已驳回 3=已取消 */
    private String status;

    /** 审批人用户ID（NULL=任意有权限者） */
    private Long approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批意见 */
    private String approveRemark;

    /** 审批时间（yyyy-MM-dd HH:mm:ss） */
    private String approveTime;
}
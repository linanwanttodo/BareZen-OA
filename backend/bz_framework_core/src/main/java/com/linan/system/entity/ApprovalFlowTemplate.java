package com.linan.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.linan.core.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程模板实体，对应 approval_flow_template 表
 *
 * <p>定义每类业务单的审批节点链；同一 biz_type 的多行（node_seq 递增）即多级审批，
 * 种子数据默认每类 1 节点（any 规则），与既有单级审批行为兼容。</p>
 *
 * @author BareZen Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow_template")
public class ApprovalFlowTemplate extends BaseEntity {

    /** 业务类型：leave=请假 overtime=加班 travel=出差 vehicle=用车 */
    private String bizType;

    /** 节点顺序（从1开始，不要求连续） */
    private Integer nodeSeq;

    /** 节点名称（如：部门主管审批 / HR审批） */
    private String nodeName;

    /** 是否启用：1=启用 0=停用 */
    private Integer enabled;
}
package com.linan.system.service;

import com.linan.system.entity.ApprovalFlowTask;

import java.util.List;

/**
 * 多级审批引擎
 *
 * <p>将「提交→审批」单级模型演进为模板驱动的多级节点链：业务提交时按
 * {@code approval_flow_template} 生成首节点任务，审批通过后按模板推进插入
 * 后续节点，末节点通过/任意节点驳回即终态。业务表 status 语义不变
 * （0=审批中，多级期间保持；1=通过；2=驳回；3=撤销），终态由业务侧负责更新。</p>
 *
 * @author BareZen Team
 */
public interface ApprovalFlowEngineService {

    /**
     * 发起审批：按模板生成首节点任务
     *
     * @param bizType       业务类型（leave/overtime/travel/vehicle）
     * @param bizId         业务记录ID
     * @param assigneeId    指定审批人用户ID（业务单 V31 assignee 字段，NULL=任意有权限者）
     * @param assigneeName  指定审批人姓名
     * @return true=已按模板生成节点链；false=该业务类型未配置模板（业务侧保持原逻辑）
     */
    boolean start(String bizType, Long bizId, Long assigneeId, String assigneeName);

    /**
     * 审批当前待审节点（通过/驳回）
     *
     * @param bizType       业务类型
     * @param bizId         业务记录ID
     * @param approverId    审批人用户ID
     * @param approverName  审批人姓名
     * @param action        审批动作：pass=通过 reject=驳回
     * @param remark        审批意见
     * @return 节点处理结果（approved/finished/nodeSeq/nodeName）
     */
    ApprovalResult approve(String bizType, Long bizId, Long approverId,
                           String approverName, String action, String remark);

    /**
     * 撤销：全部待审节点置为取消
     */
    void revoke(String bizType, Long bizId);

    /**
     * 查询某单据完整节点链（node_seq 升序，含已审/待审/取消）
     */
    List<ApprovalFlowTask> listTasks(String bizType, Long bizId);

    /**
     * 查询某单据当前待审节点（status='0'，正常仅一行；用于待办聚合/流程展示）
     */
    List<ApprovalFlowTask> currentTasks(String bizType, Long bizId);

    /**
     * 节点审批结果
     */
    class ApprovalResult {
        /** 是否通过（false=驳回） */
        private final boolean approved;
        /** 是否终态：末节点通过或任意节点驳回 */
        private final boolean finished;
        /** 当前处理节点序号 */
        private final Integer nodeSeq;
        /** 当前处理节点名称 */
        private final String nodeName;

        public ApprovalResult(boolean approved, boolean finished,
                              Integer nodeSeq, String nodeName) {
            this.approved = approved;
            this.finished = finished;
            this.nodeSeq = nodeSeq;
            this.nodeName = nodeName;
        }

        public boolean isApproved() {
            return approved;
        }

        public boolean isFinished() {
            return finished;
        }

        public Integer getNodeSeq() {
            return nodeSeq;
        }

        public String getNodeName() {
            return nodeName;
        }
    }
}
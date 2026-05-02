package com.linan.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.ApprovalFlowLog;

/**
 * 审批流程历史服务接口
 *
 * @author BareZen Team
 */
public interface IApprovalFlowLogService extends IService<ApprovalFlowLog> {

    /**
     * 记录审批流程节点
     *
     * @param bizType      业务类型（leave/overtime/travel/vehicle/supply）
     * @param bizId        业务记录ID
     * @param nodeName     节点名称
     * @param nodeStatus   节点后状态（0/1/2/3）
     * @param operatorId   操作人用户ID
     * @param operatorName 操作人姓名
     * @param remark       备注（审批意见等）
     */
    void record(String bizType, Long bizId, String nodeName, String nodeStatus,
                Long operatorId, String operatorName, String remark);
}

package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.ApprovalFlowLog;
import com.linan.system.mapper.ApprovalFlowLogMapper;
import com.linan.system.service.IApprovalFlowLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审批流程历史服务实现
 *
 * @author BareZen Team
 */
@Slf4j
@Service
public class ApprovalFlowLogServiceImpl extends ServiceImpl<ApprovalFlowLogMapper, ApprovalFlowLog>
        implements IApprovalFlowLogService {

    @Override
    public void record(String bizType, Long bizId, String nodeName, String nodeStatus,
                       Long operatorId, String operatorName, String remark) {
        try {
            ApprovalFlowLog log = new ApprovalFlowLog();
            log.setBizType(bizType);
            log.setBizId(bizId);
            log.setNodeName(nodeName);
            log.setNodeStatus(nodeStatus);
            log.setOperatorId(operatorId);
            log.setOperatorName(operatorName);
            log.setRemark(remark);
            save(log);
        } catch (Exception e) {
            // 流程记录失败不影响主业务，仅告警
            log.warn("记录审批流程节点失败: bizType={}, bizId={}, node={}", bizType, bizId, nodeName, e);
        }
    }
}

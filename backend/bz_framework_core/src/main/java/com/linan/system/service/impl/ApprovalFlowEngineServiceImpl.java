package com.linan.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.entity.ApprovalFlowTemplate;
import com.linan.system.service.ApprovalFlowEngineService;
import com.linan.system.service.IApprovalFlowTaskService;
import com.linan.system.service.IApprovalFlowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * 多级审批引擎实现
 *
 * <p>核心不变量：同一 (bizType, bizId) 在同一时刻至多一行 task.status='0'
 * （当前待审节点）。审批通过后按模板 node_seq 递增推进式插入下一节点；
 * 并发重复审批通过原子条件更新（WHERE status='0'）保证仅首个生效。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowEngineServiceImpl implements ApprovalFlowEngineService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String STATUS_PENDING = "0";
    private static final String STATUS_APPROVED = "1";
    private static final String STATUS_REJECTED = "2";
    private static final String STATUS_CANCELED = "3";

    private final IApprovalFlowTemplateService templateService;
    private final IApprovalFlowTaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean start(String bizType, Long bizId, Long assigneeId, String assigneeName) {
        List<ApprovalFlowTemplate> nodes = enabledTemplates(bizType);
        if (nodes.isEmpty()) {
            log.warn("审批模板未配置: bizType={}, bizId={}", bizType, bizId);
            return false;
        }
        ApprovalFlowTemplate first = nodes.get(0);
        ApprovalFlowTask task = new ApprovalFlowTask();
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setNodeSeq(first.getNodeSeq());
        task.setNodeName(first.getNodeName());
        task.setStatus(STATUS_PENDING);
        // 指定审批人（业务单 V31 assignee 字段）仅作用于第一节点
        task.setApproverId(assigneeId);
        task.setApproverName(assigneeName);
        taskService.save(task);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalResult approve(String bizType, Long bizId, Long approverId,
                                  String approverName, String action, String remark) {
        boolean approved = "pass".equals(action);
        if (!approved && !"reject".equals(action)) {
            throw new BusinessException("无效的审批动作");
        }

        ApprovalFlowTask current = currentTask(bizType, bizId);
        if (current == null) {
            throw new BusinessException("无待审批节点，可能已处理");
        }
        // 指定审批人校验：节点指定了审批人时仅其可操作
        if (current.getApproverId() != null && !current.getApproverId().equals(approverId)) {
            throw new BusinessException("该节点已指定审批人，仅其可审批");
        }

        // 原子条件更新：并发重复审批仅首个生效（0 行 = 已被并发处理）
        boolean ok = taskService.update(new LambdaUpdateWrapper<ApprovalFlowTask>()
                .eq(ApprovalFlowTask::getId, current.getId())
                .eq(ApprovalFlowTask::getStatus, STATUS_PENDING)
                .set(ApprovalFlowTask::getStatus, approved ? STATUS_APPROVED : STATUS_REJECTED)
                .set(ApprovalFlowTask::getApproverId, approverId)
                .set(ApprovalFlowTask::getApproverName, approverName)
                .set(ApprovalFlowTask::getApproveRemark, remark)
                .set(ApprovalFlowTask::getApproveTime, LocalDateTime.now().format(FMT)));
        if (!ok) {
            throw new BusinessException("该节点已处理，不能重复操作");
        }

        // 驳回即终态
        if (!approved) {
            return new ApprovalResult(false, true, current.getNodeSeq(), current.getNodeName());
        }

        // 通过：查模板找下一节点；无则末节点通过 = 终态
        ApprovalFlowTemplate next = nextTemplate(bizType, current.getNodeSeq());
        if (next == null) {
            return new ApprovalResult(true, true, current.getNodeSeq(), current.getNodeName());
        }
        ApprovalFlowTask nextTask = new ApprovalFlowTask();
        nextTask.setBizType(bizType);
        nextTask.setBizId(bizId);
        nextTask.setNodeSeq(next.getNodeSeq());
        nextTask.setNodeName(next.getNodeName());
        nextTask.setStatus(STATUS_PENDING);
        taskService.save(nextTask);
        return new ApprovalResult(true, false, current.getNodeSeq(), current.getNodeName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String bizType, Long bizId) {
        taskService.update(new LambdaUpdateWrapper<ApprovalFlowTask>()
                .eq(ApprovalFlowTask::getBizType, bizType)
                .eq(ApprovalFlowTask::getBizId, bizId)
                .eq(ApprovalFlowTask::getStatus, STATUS_PENDING)
                .set(ApprovalFlowTask::getStatus, STATUS_CANCELED));
    }

    @Override
    public List<ApprovalFlowTask> listTasks(String bizType, Long bizId) {
        return taskService.list(new LambdaQueryWrapper<ApprovalFlowTask>()
                .eq(ApprovalFlowTask::getBizType, bizType)
                .eq(ApprovalFlowTask::getBizId, bizId)
                .orderByAsc(ApprovalFlowTask::getNodeSeq));
    }

    @Override
    public List<ApprovalFlowTask> currentTasks(String bizType, Long bizId) {
        return taskService.list(new LambdaQueryWrapper<ApprovalFlowTask>()
                .eq(ApprovalFlowTask::getBizType, bizType)
                .eq(ApprovalFlowTask::getBizId, bizId)
                .eq(ApprovalFlowTask::getStatus, STATUS_PENDING)
                .orderByAsc(ApprovalFlowTask::getNodeSeq));
    }

    /** 当前待审节点（status='0' 的第一条） */
    private ApprovalFlowTask currentTask(String bizType, Long bizId) {
        return taskService.getOne(new LambdaQueryWrapper<ApprovalFlowTask>()
                .eq(ApprovalFlowTask::getBizType, bizType)
                .eq(ApprovalFlowTask::getBizId, bizId)
                .eq(ApprovalFlowTask::getStatus, STATUS_PENDING)
                .orderByAsc(ApprovalFlowTask::getNodeSeq)
                .last("LIMIT 1"), false);
    }

    /** 某业务类型启用中的模板节点（node_seq 升序） */
    private List<ApprovalFlowTemplate> enabledTemplates(String bizType) {
        return templateService.list(new LambdaQueryWrapper<ApprovalFlowTemplate>()
                .eq(ApprovalFlowTemplate::getBizType, bizType)
                .eq(ApprovalFlowTemplate::getEnabled, 1)
                .orderByAsc(ApprovalFlowTemplate::getNodeSeq));
    }

    /** 模板中 node_seq 大于当前值的下一节点（不要求连续） */
    private ApprovalFlowTemplate nextTemplate(String bizType, Integer currentSeq) {
        return enabledTemplates(bizType).stream()
                .filter(t -> t.getNodeSeq() > currentSeq)
                .min(Comparator.comparing(ApprovalFlowTemplate::getNodeSeq))
                .orElse(null);
    }
}
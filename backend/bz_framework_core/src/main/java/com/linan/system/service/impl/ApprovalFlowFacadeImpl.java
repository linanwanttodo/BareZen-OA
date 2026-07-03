package com.linan.system.service.impl;

import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.service.ApprovalFlowEngineService;
import com.linan.system.service.ApprovalFlowFacade;
import com.linan.system.service.IApprovalFlowLogService;
import com.linan.core.spi.permission.PermissionGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 审批流程编排门面实现
 *
 * <p>统一编排：引擎推进（{@link ApprovalFlowEngineService}）、终态业务表更新
 * （{@link ApprovalBizHandler}）、流程日志（{@link IApprovalFlowLogService}）、
 * 通知事件（{@link ApplicationEventPublisher}）。所有写入在同一事务内，保证业务表、
 * 审批节点、流程日志原子一致，消除各审批 Controller 的重复编排。</p>
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class ApprovalFlowFacadeImpl implements ApprovalFlowFacade {

    /** 管理员代审开关（默认关闭）。开启后 admin 可审批自己提交的申请，日志中标注「管理员代审（自审）」。 */
    @Value("${approval.self-approve.enabled:false}")
    private boolean selfApproveEnabled;

    private final ApprovalFlowEngineService flowEngine;
    private final IApprovalFlowLogService flowLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final PermissionGateway permissionGateway;

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void submit(String bizType, Long bizId, Long assigneeId, String assigneeName,
                       Long operatorId, String operatorName) {
        flowEngine.start(bizType, bizId, assigneeId, assigneeName);
        flowLogService.record(bizType, bizId, "提交申请",
                ApprovalStatus.PENDING.getValue(), operatorId, operatorName, null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void revoke(String bizType, Long bizId, Long operatorId, String operatorName,
                       ApprovalBizHandler handler) {
        // 仅本人可撤销
        if (operatorId == null || !operatorId.equals(handler.applicantId())) {
            throw new BusinessException("只能撤销自己的申请");
        }
        boolean ok = handler.cancel(); // 业务表 0→3（原子条件，防并发覆盖）
        if (!ok) {
            // 失败留痕：记录单据当前实际状态（可能已被审批改走），保证可审计
            flowLogService.record(bizType, bizId, "撤销失败", handler.currentStatus(),
                    operatorId, operatorName, "仅待审批状态的申请可撤销");
            throw new BusinessException("仅待审批状态的申请可撤销");
        }
        flowEngine.revoke(bizType, bizId);
        flowLogService.record(bizType, bizId, "撤销申请",
                ApprovalStatus.CANCELED.getValue(), operatorId, operatorName, null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void approve(String bizType, Long bizId, Long approverId, String approverName,
                        String action, String remark, ApprovalBizHandler handler) {
        // 禁止自审自批。默认对所有人生效；仅当 approval.self-approve.enabled=true
        // 且审批人持有 admin 角色时放行（小微团队里管理员往往是唯一审批人，
        // 完全禁止会导致流程死锁），放行时在流程日志中标注「管理员代审（自审）」保持可审计。
        boolean isSelfApproval = approverId != null && approverId.equals(handler.applicantId());
        boolean adminOverride = isSelfApproval && selfApproveEnabled
                && approverId != null && permissionGateway.isAdmin(approverId);
        if (isSelfApproval && !adminOverride) {
            throw new BusinessException("不能审批自己的申请");
        }

        ApprovalFlowEngineService.ApprovalResult result =
                flowEngine.approve(bizType, bizId, approverId, approverName, action, remark);

        // 管理员代审时在节点名后缀标注，普通审批保持原样
        String nodeSuffix = adminOverride ? "（管理员代审）" : "";

        if (result.isFinished()) {
            // 终态：驳回或末节点通过，原子更新业务表（并发兜底：后到者受影响行数为 0）
            String target = result.isApproved()
                    ? ApprovalStatus.APPROVED.getValue()
                    : ApprovalStatus.REJECTED.getValue();
            boolean ok = handler.finish(target, approverId, approverName, remark);
            if (!ok) {
                throw new BusinessException("该记录已审批，不能重复操作");
            }
            flowLogService.record(bizType, bizId,
                    (result.isApproved() ? "审批通过" : "审批驳回") + nodeSuffix,
                    target, approverId, approverName, remark);
            // 结果通知申请人（通过/驳回均推送）
            ApprovalFlowFacade.NotifyText txt = handler.resultNotify(result.isApproved(), remark);
            eventPublisher.publishEvent(new ApprovalCompletedEvent(
                    handler.applicantId(), txt.title(), txt.content()));
        } else {
            // 中间节点通过：业务表保持待审批，若下一节点指定了审批人则通知其办理
            List<ApprovalFlowTask> nextTasks = flowEngine.currentTasks(bizType, bizId);
            ApprovalFlowFacade.NotifyText txt = handler.nextNotify();
            for (ApprovalFlowTask task : nextTasks) {
                if (task.getApproverId() != null) {
                    eventPublisher.publishEvent(new ApprovalCompletedEvent(
                            task.getApproverId(), txt.title(), txt.content()));
                }
            }
        }
    }
}
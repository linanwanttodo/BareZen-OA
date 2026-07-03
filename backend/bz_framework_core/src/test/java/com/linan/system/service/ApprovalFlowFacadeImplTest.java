package com.linan.system.service;

import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.service.impl.ApprovalFlowFacadeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ApprovalFlowFacadeImpl 单元测试
 *
 * <p>覆盖审批编排骨架（此前分散在各审批 Controller 中的重复逻辑）：
 * 提交（引擎启动+日志）、撤销（归属校验/业务表原子更新失败留痕含实际状态/成功留痕）、
 * 审批（自审拒绝/终态更新+日志+通知/中间节点催办/并发兜底）、方法级事务标注。</p>
 */
@ExtendWith(MockitoExtension.class)
class ApprovalFlowFacadeImplTest {

    private static final String BIZ_TYPE = "leave";
    private static final Long BIZ_ID = 1001L;
    private static final Long OPERATOR_ID = 2L;
    private static final String OPERATOR_NAME = "张三";
    private static final Long APPROVER_ID = 1L;
    private static final String APPROVER_NAME = "admin";

    @Mock
    private ApprovalFlowEngineService flowEngine;

    @Mock
    private IApprovalFlowLogService flowLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.linan.core.spi.permission.PermissionGateway permissionGateway;

    private ApprovalFlowFacade facade;

    /** 默认：管理员代审开关关闭（与 application.yml 默认值一致） */
    private ApprovalFlowFacadeImpl newFacade() {
        return newFacade(false);
    }

    private ApprovalFlowFacadeImpl newFacade(boolean selfApproveEnabled) {
        ApprovalFlowFacadeImpl f = new ApprovalFlowFacadeImpl(
                flowEngine, flowLogService, eventPublisher, permissionGateway);
        try {
            var field = ApprovalFlowFacadeImpl.class.getDeclaredField("selfApproveEnabled");
            field.setAccessible(true);
            field.set(f, selfApproveEnabled);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("注入 selfApproveEnabled 失败", e);
        }
        return f;
    }

    /** 无自定义回调的默认 handler（applicantId 非 OPERATOR_ID，供自审/归属用例自定义） */
    private ApprovalFlowFacade.ApprovalBizHandler handler(Long applicantId, boolean cancelOk) {
        return new ApprovalFlowFacade.ApprovalBizHandler() {
            @Override
            public Long applicantId() {
                return applicantId;
            }

            @Override
            public boolean cancel() {
                return cancelOk;
            }

            @Override
            public String currentStatus() {
                return ApprovalStatus.APPROVED.getValue();
            }
        };
    }

    // ==================== submit ====================

    @Test
    @DisplayName("submit：启动引擎首节点 + 记录提交申请日志（待审批状态）")
    void submit_startsEngineAndLogs() {
        facade = newFacade();

        facade.submit(BIZ_TYPE, BIZ_ID, 50L, "李指定", OPERATOR_ID, OPERATOR_NAME);

        verify(flowEngine).start(BIZ_TYPE, BIZ_ID, 50L, "李指定");
        verify(flowLogService).record(BIZ_TYPE, BIZ_ID, "提交申请",
                ApprovalStatus.PENDING.getValue(), OPERATOR_ID, OPERATOR_NAME, null);
    }

    // ==================== revoke ====================

    @Test
    @DisplayName("revoke：非本人撤销抛业务异常，不触达引擎与日志")
    void revoke_notOwner_rejected() {
        facade = newFacade();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.revoke(BIZ_TYPE, BIZ_ID, 999L, "他人", handler(OPERATOR_ID, true)));

        assertTrue(ex.getMessage().contains("只能撤销自己的申请"));
        verify(flowEngine, never()).revoke(any(), any());
        verify(flowLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("revoke：业务表原子更新失败（并发已处理）→ 失败留痕含实际状态 + 抛业务异常")
    void revoke_conditionalUpdateFailed_logsWithActualStatus() {
        facade = newFacade();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.revoke(BIZ_TYPE, BIZ_ID, OPERATOR_ID, OPERATOR_NAME, handler(OPERATOR_ID, false)));

        assertTrue(ex.getMessage().contains("仅待审批状态的申请可撤销"));
        // 失败留痕：节点=撤销失败，状态=单据当前实际状态（此处 mock 返回已通过）
        verify(flowLogService).record(BIZ_TYPE, BIZ_ID, "撤销失败",
                ApprovalStatus.APPROVED.getValue(), OPERATOR_ID, OPERATOR_NAME, "仅待审批状态的申请可撤销");
        verify(flowEngine, never()).revoke(any(), any());
    }

    @Test
    @DisplayName("revoke：成功撤销 → 业务表已更新 + 引擎节点取消 + 撤销日志")
    void revoke_success_cancelsEngineAndLogs() {
        facade = newFacade();

        facade.revoke(BIZ_TYPE, BIZ_ID, OPERATOR_ID, OPERATOR_NAME, handler(OPERATOR_ID, true));

        verify(flowEngine).revoke(BIZ_TYPE, BIZ_ID);
        verify(flowLogService).record(BIZ_TYPE, BIZ_ID, "撤销申请",
                ApprovalStatus.CANCELED.getValue(), OPERATOR_ID, OPERATOR_NAME, null);
    }

    // ==================== approve ====================

    @Test
    @DisplayName("approve：自审自批被拒绝，不触达引擎")
    void approve_selfApproval_rejected() {
        facade = newFacade();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", null,
                        handler(APPROVER_ID, true)));

        assertTrue(ex.getMessage().contains("不能审批自己的申请"));
        verify(flowEngine, never()).approve(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("approve：代审开关开启但审批人非 admin → 仍拒绝自审")
    void approve_selfApproval_switchOnButNotAdmin_rejected() {
        facade = newFacade(true);
        when(permissionGateway.isAdmin(APPROVER_ID)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", null,
                        handler(APPROVER_ID, true)));

        assertTrue(ex.getMessage().contains("不能审批自己的申请"));
        verify(flowEngine, never()).approve(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("approve：代审开关开启 + 审批人是 admin → 放行且日志标注「管理员代审」")
    void approve_selfApproval_adminOverride_allowedAndMarked() {
        facade = newFacade(true);
        when(permissionGateway.isAdmin(APPROVER_ID)).thenReturn(true);
        when(flowEngine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "代审通过"))
                .thenReturn(new ApprovalFlowEngineService.ApprovalResult(true, true, 1, "主管审批"));

        facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "代审通过",
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return APPROVER_ID;
                    }

                    @Override
                    public boolean cancel() {
                        return true;
                    }

                    @Override
                    public String currentStatus() {
                        return ApprovalStatus.PENDING.getValue();
                    }

                    @Override
                    public boolean finish(String targetStatus, Long id, String name, String remark) {
                        return true;
                    }
                });

        verify(flowLogService).record(BIZ_TYPE, BIZ_ID, "审批通过（管理员代审）",
                ApprovalStatus.APPROVED.getValue(), APPROVER_ID, APPROVER_NAME, "代审通过");
    }

    @Test
    @DisplayName("approve：代审开关关闭时 admin 也被拒绝（默认内控口径，且不查 admin 身份）")
    void approve_selfApproval_switchOff_adminAlsoRejected() {
        facade = newFacade(false);
        // 不 stub isAdmin：开关关闭时实现根本不该调用它，
        // 若误调用会因返回默认值 false 而让本用例「碰巧通过」，掩盖回归。
        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", null,
                        handler(APPROVER_ID, true)));

        assertTrue(ex.getMessage().contains("不能审批自己的申请"));
        verify(flowEngine, never()).approve(any(), any(), any(), any(), any(), any());
        verify(permissionGateway, never()).isAdmin(any());
    }

    @Test
    @DisplayName("approve：终态通过 → 业务表更新 + 审批通过日志 + 结果通知（仅站内推送）")
    void approve_finishedPassed_updatesLogsNotifies() {
        facade = newFacade();
        when(flowEngine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意"))
                .thenReturn(new ApprovalFlowEngineService.ApprovalResult(true, true, 1, "主管审批"));

        facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意",
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return OPERATOR_ID;
                    }

                    @Override
                    public boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
                        return true;
                    }

                    @Override
                    public boolean cancel() {
                        return false;
                    }

                    @Override
                    public ApprovalFlowFacade.NotifyText resultNotify(boolean approved, String remark) {
                        return ApprovalFlowFacade.NotifyText.of("审批通过", "您的申请已通过");
                    }
                });

        verify(flowLogService).record(BIZ_TYPE, BIZ_ID, "审批通过",
                ApprovalStatus.APPROVED.getValue(), APPROVER_ID, APPROVER_NAME, "同意");
        verify(eventPublisher).publishEvent(any(ApprovalCompletedEvent.class));
    }

    @Test
    @DisplayName("approve：终态驳回 → 业务表置驳回 + 驳回通知")
    void approve_finishedRejected_logsRejectAndNotifies() {
        facade = newFacade();
        when(flowEngine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "reject", "理由不足"))
                .thenReturn(new ApprovalFlowEngineService.ApprovalResult(false, true, 1, "主管审批"));

        facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "reject", "理由不足",
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return OPERATOR_ID;
                    }

                    @Override
                    public boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
                        return true;
                    }

                    @Override
                    public boolean cancel() {
                        return false;
                    }
                });

        verify(flowLogService).record(BIZ_TYPE, BIZ_ID, "审批驳回",
                ApprovalStatus.REJECTED.getValue(), APPROVER_ID, APPROVER_NAME, "理由不足");
        verify(eventPublisher).publishEvent(any(ApprovalCompletedEvent.class));
    }

    @Test
    @DisplayName("approve：终态但业务表并发更新失败 → 抛业务异常，不写日志不发通知")
    void approve_finishConcurrentConflict_rejected() {
        facade = newFacade();
        when(flowEngine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", null))
                .thenReturn(new ApprovalFlowEngineService.ApprovalResult(true, true, 1, "主管审批"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", null,
                        new ApprovalFlowFacade.ApprovalBizHandler() {
                            @Override
                            public Long applicantId() {
                                return OPERATOR_ID;
                            }

                            @Override
                            public boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
                                return false; // 并发兜底：受影响行数为 0
                            }

                            @Override
                            public boolean cancel() {
                                return false;
                            }
                        }));

        assertTrue(ex.getMessage().contains("该记录已审批"));
        verify(flowLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("approve：中间节点通过 → 业务表不变更，仅催办指定审批人")
    void approve_intermediate_pushesNextApprover() {
        facade = newFacade();
        when(flowEngine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意"))
                .thenReturn(new ApprovalFlowEngineService.ApprovalResult(true, false, 1, "主管审批"));
        ApprovalFlowTask next = new ApprovalFlowTask();
        next.setApproverId(88L);
        when(flowEngine.currentTasks(BIZ_TYPE, BIZ_ID)).thenReturn(List.of(next));

        facade.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意",
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return OPERATOR_ID;
                    }

                    @Override
                    public boolean cancel() {
                        return false;
                    }

                    @Override
                    public ApprovalFlowFacade.NotifyText nextNotify() {
                        return ApprovalFlowFacade.NotifyText.of("新的待审批申请", "有新申请待您审批");
                    }
                });

        // 中间节点不写终态日志、不发结果通知
        verify(flowLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
        // 下一节点指定审批人 → 催办通知
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        Object published = captor.getValue();
        assertTrue(published instanceof ApprovalCompletedEvent, "必须发布审批完成事件");
        assertEquals(88L, ((ApprovalCompletedEvent) published).getUserId(), "催办对象为下一节点指定审批人");
    }

    // ==================== 事务 ====================

    @Test
    @DisplayName("submit/revoke/approve 均声明 @Transactional(rollbackFor=Exception)")
    void orchestrationMethods_areTransactional() throws Exception {
        Method submit = ApprovalFlowFacadeImpl.class.getMethod("submit", String.class, Long.class,
                Long.class, String.class, Long.class, String.class);
        Method revoke = ApprovalFlowFacadeImpl.class.getMethod("revoke", String.class, Long.class,
                Long.class, String.class, ApprovalFlowFacade.ApprovalBizHandler.class);
        Method approve = ApprovalFlowFacadeImpl.class.getMethod("approve", String.class, Long.class,
                Long.class, String.class, String.class, String.class, ApprovalFlowFacade.ApprovalBizHandler.class);
        for (Method m : new Method[]{submit, revoke, approve}) {
            org.springframework.transaction.annotation.Transactional tx =
                    m.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
            assertNotNull(tx, m.getName() + " 必须声明 @Transactional");
            assertEquals(1, tx.rollbackFor().length, m.getName() + " 必须声明 rollbackFor=Exception");
            assertEquals(Exception.class, tx.rollbackFor()[0],
                    m.getName() + " 必须声明 rollbackFor=Exception");
        }
    }
}
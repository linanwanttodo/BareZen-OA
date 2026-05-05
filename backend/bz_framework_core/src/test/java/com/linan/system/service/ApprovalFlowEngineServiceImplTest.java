package com.linan.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.exception.BusinessException;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.entity.ApprovalFlowTemplate;
import com.linan.system.service.impl.ApprovalFlowEngineServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ApprovalFlowEngineServiceImpl 单元测试
 *
 * <p>覆盖：发起审批（有/无模板、指定审批人）；审批推进（单节点终态/多级推进/
 * 末节点终态/驳回终态）；校验（指定审批人/无效动作/无待审节点）；并发原子更新；
 * 撤销；节点链查询。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class ApprovalFlowEngineServiceImplTest {

    private static final String BIZ_TYPE = "leave";
    private static final Long BIZ_ID = 1001L;
    private static final Long APPROVER_ID = 10L;
    private static final String APPROVER_NAME = "王审批";

    @Mock
    private IApprovalFlowTemplateService templateService;

    @Mock
    private IApprovalFlowTaskService taskService;

    private ApprovalFlowEngineServiceImpl engine;

    @BeforeAll
    static void initTableInfoCache() {
        // 初始化实体列映射，使 LambdaWrapper 在纯 Mockito 环境下可用
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApprovalFlowTask.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApprovalFlowTemplate.class);
    }

    @BeforeEach
    void setUp() {
        engine = new ApprovalFlowEngineServiceImpl(templateService, taskService);
    }

    /** 构造模板节点 */
    private ApprovalFlowTemplate tpl(Integer seq, String name) {
        ApprovalFlowTemplate t = new ApprovalFlowTemplate();
        t.setBizType(BIZ_TYPE);
        t.setNodeSeq(seq);
        t.setNodeName(name);
        t.setEnabled(1);
        return t;
    }

    /** 构造节点任务 */
    private ApprovalFlowTask task(Integer seq, String status, Long approverId) {
        ApprovalFlowTask t = new ApprovalFlowTask();
        t.setId(seq.longValue());
        t.setBizType(BIZ_TYPE);
        t.setBizId(BIZ_ID);
        t.setNodeSeq(seq);
        t.setNodeName("节点" + seq);
        t.setStatus(status);
        t.setApproverId(approverId);
        return t;
    }

    @Nested
    @DisplayName("start() 发起审批")
    class Start {

        @Test
        @DisplayName("有模板生成首节点任务（含指定审批人）")
        void withTemplate_createsFirstTask() {
            when(templateService.list(any(Wrapper.class))).thenReturn(List.of(tpl(1, "请假审批")));
            when(taskService.save(any(ApprovalFlowTask.class))).thenReturn(true);

            boolean started = engine.start(BIZ_TYPE, BIZ_ID, 50L, "李指定");

            assertTrue(started);
            ArgumentCaptor<ApprovalFlowTask> captor = ArgumentCaptor.forClass(ApprovalFlowTask.class);
            verify(taskService).save(captor.capture());
            ApprovalFlowTask saved = captor.getValue();
            assertEquals(BIZ_TYPE, saved.getBizType());
            assertEquals(BIZ_ID, saved.getBizId());
            assertEquals(1, saved.getNodeSeq());
            assertEquals("请假审批", saved.getNodeName());
            assertEquals("0", saved.getStatus());
            assertEquals(50L, saved.getApproverId(), "指定审批人落到首节点");
            assertEquals("李指定", saved.getApproverName());
        }

        @Test
        @DisplayName("无模板返回 false 且不生成任务（兼容未配置业务）")
        void noTemplate_returnsFalse() {
            when(templateService.list(any(Wrapper.class))).thenReturn(List.of());

            assertFalse(engine.start(BIZ_TYPE, BIZ_ID, null, null));
            verify(taskService, never()).save(any());
        }

        @Test
        @DisplayName("多级模板仅生成首节点（node_seq 最小）")
        void multiLevelTemplate_onlyFirstNode() {
            when(templateService.list(any(Wrapper.class))).thenReturn(
                    List.of(tpl(1, "主管审批"), tpl(2, "HR审批"), tpl(3, "总经理审批")));
            when(taskService.save(any(ApprovalFlowTask.class))).thenReturn(true);

            engine.start(BIZ_TYPE, BIZ_ID, null, null);

            ArgumentCaptor<ApprovalFlowTask> captor = ArgumentCaptor.forClass(ApprovalFlowTask.class);
            verify(taskService).save(captor.capture());
            assertEquals(1, captor.getValue().getNodeSeq());
            assertEquals("主管审批", captor.getValue().getNodeName());
        }
    }

    @Nested
    @DisplayName("approve() 审批推进")
    class Approve {

        @Test
        @DisplayName("单级模板首节点通过即终态")
        void singleLevel_passFinished() {
            when(taskService.getOne(any(), anyBoolean())).thenReturn(task(1, "0", null));
            when(taskService.update(any())).thenReturn(true);
            when(templateService.list(any(Wrapper.class))).thenReturn(List.of(tpl(1, "请假审批")));

            ApprovalFlowEngineService.ApprovalResult result =
                    engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意");

            assertTrue(result.isApproved());
            assertTrue(result.isFinished(), "单节点通过即终态");
            verify(taskService, never()).save(any());
        }

        @Test
        @DisplayName("多级模板首节点通过：未终态，插入下一节点任务")
        void multiLevel_firstNode_passesAndActivatesNext() {
            when(taskService.getOne(any(), anyBoolean())).thenReturn(task(1, "0", null));
            when(taskService.update(any())).thenReturn(true);
            when(templateService.list(any(Wrapper.class))).thenReturn(
                    List.of(tpl(1, "主管审批"), tpl(2, "HR审批")));
            when(taskService.save(any(ApprovalFlowTask.class))).thenReturn(true);

            ApprovalFlowEngineService.ApprovalResult result =
                    engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意");

            assertTrue(result.isApproved());
            assertFalse(result.isFinished(), "还有下一节点，审批未完成");
            ArgumentCaptor<ApprovalFlowTask> captor = ArgumentCaptor.forClass(ApprovalFlowTask.class);
            verify(taskService).save(captor.capture());
            assertEquals(2, captor.getValue().getNodeSeq());
            assertEquals("HR审批", captor.getValue().getNodeName());
            assertEquals("0", captor.getValue().getStatus());
        }

        @Test
        @DisplayName("末节点通过即终态")
        void lastNode_passFinished() {
            when(taskService.getOne(any(), anyBoolean())).thenReturn(task(2, "0", null));
            when(taskService.update(any())).thenReturn(true);
            when(templateService.list(any(Wrapper.class))).thenReturn(
                    List.of(tpl(1, "主管审批"), tpl(2, "HR审批")));

            ApprovalFlowEngineService.ApprovalResult result =
                    engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意");

            assertTrue(result.isApproved());
            assertTrue(result.isFinished(), "末节点通过即终态");
            verify(taskService, never()).save(any());
        }

        @Test
        @DisplayName("驳回即终态，不生成后续节点")
        void reject_finished() {
            when(taskService.getOne(any(), anyBoolean())).thenReturn(task(1, "0", null));
            when(taskService.update(any())).thenReturn(true);

            ApprovalFlowEngineService.ApprovalResult result =
                    engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "reject", "不同意");

            assertFalse(result.isApproved());
            assertTrue(result.isFinished(), "驳回即终态");
            verify(taskService, never()).save(any());
        }

        @Test
        @DisplayName("指定审批人节点：非指定人审批被拒绝")
        void assignee_mismatchRejected() {
            when(taskService.getOne(any(), anyBoolean())).thenReturn(task(1, "0", 50L));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", "同意"));
            assertTrue(ex.getMessage().contains("指定审批人"));
        }

        @Test
        @DisplayName("无待审节点拒绝（已终态或未发起）")
        void noPendingTask_rejected() {
            when(taskService.getOne(any(), anyBoolean())).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", null));
            assertTrue(ex.getMessage().contains("无待审批节点"));
        }

        @Test
        @DisplayName("无效审批动作拒绝")
        void invalidAction_rejected() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "xxx", null));
            assertTrue(ex.getMessage().contains("无效"));
        }

        @Test
        @DisplayName("并发冲突（原子更新 0 行）拒绝重复审批")
        void concurrent_conflictRejected() {
            when(taskService.getOne(any(), anyBoolean())).thenReturn(task(1, "0", null));
            when(taskService.update(any())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> engine.approve(BIZ_TYPE, BIZ_ID, APPROVER_ID, APPROVER_NAME, "pass", null));
            assertTrue(ex.getMessage().contains("已处理"));
        }
    }

    @Nested
    @DisplayName("revoke() / 查询")
    class RevokeAndQuery {

        @Test
        @DisplayName("撤销：待审节点置取消")
        void revoke_cancelsPending() {
            when(taskService.update(any())).thenReturn(true);

            engine.revoke(BIZ_TYPE, BIZ_ID);

            ArgumentCaptor<LambdaUpdateWrapper<ApprovalFlowTask>> captor =
                    ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
            verify(taskService).update(captor.capture());
            String sql = captor.getValue().getSqlSegment();
            assertTrue(sql.contains("status"), "按待审状态条件更新");
        }

        @Test
        @DisplayName("listTasks 按节点顺序返回完整链")
        void listTasks_orderedChain() {
            when(taskService.list(any(Wrapper.class))).thenReturn(List.of(task(1, "1", APPROVER_ID), task(2, "0", null)));

            List<ApprovalFlowTask> tasks = engine.listTasks(BIZ_TYPE, BIZ_ID);

            assertEquals(2, tasks.size());
            assertEquals(1, tasks.get(0).getNodeSeq());
            assertEquals(2, tasks.get(1).getNodeSeq());
        }

        @Test
        @DisplayName("currentTasks 仅查待审节点")
        void currentTasks_onlyPending() {
            when(taskService.list(any(Wrapper.class))).thenReturn(List.of(task(2, "0", null)));

            List<ApprovalFlowTask> tasks = engine.currentTasks(BIZ_TYPE, BIZ_ID);

            assertEquals(1, tasks.size());
            assertEquals("0", tasks.get(0).getStatus());
            ArgumentCaptor<LambdaQueryWrapper<ApprovalFlowTask>> captor =
                    ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(taskService).list(captor.capture());
            assertTrue(captor.getValue().getSqlSegment().contains("status"));
        }
    }
}
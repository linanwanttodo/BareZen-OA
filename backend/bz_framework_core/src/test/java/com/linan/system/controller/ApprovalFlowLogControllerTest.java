package com.linan.system.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.ApprovalFlowLog;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.service.ApprovalFlowEngineService;
import com.linan.system.service.IApprovalFlowLogService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审批流程进度查询 Controller 单元测试（2.1）
 *
 * <p>覆盖：事件流查询按 bizType/bizId 过滤转发、节点链委托引擎、
 * 未接入引擎的业务（supply）返回空链不回退异常。</p>
 */
@ExtendWith(MockitoExtension.class)
class ApprovalFlowLogControllerTest {

    @Mock
    private IApprovalFlowLogService flowLogService;

    @Mock
    private ApprovalFlowEngineService flowEngine;

    @InjectMocks
    private ApprovalFlowLogController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ApprovalFlowLog.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("事件流：按 bizType + bizId 精确过滤并转发")
    void list_filtersByBizTypeAndBizId() {
        ApprovalFlowLog log = new ApprovalFlowLog();
        when(flowLogService.list(any(Wrapper.class))).thenReturn(List.of(log));

        Result<List<ApprovalFlowLog>> result = controller.list("leave", 42L);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        verify(flowLogService).list(argThat((Wrapper<ApprovalFlowLog> w) -> {
            String sql = w.getTargetSql();
            return sql.contains("biz_type") && sql.contains("biz_id");
        }));
    }

    @Test
    @DisplayName("节点链：委托引擎查询并透传 bizType/bizId")
    void tasks_delegatesToEngine() {
        ApprovalFlowTask task = new ApprovalFlowTask();
        when(flowEngine.listTasks(eq("leave"), eq(7L))).thenReturn(List.of(task));

        Result<List<ApprovalFlowTask>> result = controller.tasks("leave", 7L);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        verify(flowEngine).listTasks("leave", 7L);
    }

    @Test
    @DisplayName("节点链：未接入引擎的业务（supply）返回空链")
    void tasks_supply_returnsEmptyChain() {
        when(flowEngine.listTasks(eq("supply"), eq(1L))).thenReturn(List.of());

        Result<List<ApprovalFlowTask>> result = controller.tasks("supply", 1L);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty(), "supply 未接入引擎，应返回空链供前端回退展示事件流");
    }
}
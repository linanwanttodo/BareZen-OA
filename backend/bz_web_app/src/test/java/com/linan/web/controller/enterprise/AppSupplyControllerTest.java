package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.IApprovalFlowLogService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppSupplyController 撤销/提交接口单元测试
 *
 * <p>覆盖 T4：申领撤销改为原子条件更新，撤销成功写流程留痕，撤销失败也必须写 flow_log 留痕（含实际状态）；</p>
 * <p>覆盖 T6：提交申领 userName/userId/status 服务端强制覆盖，不信任请求体身份字段。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppSupplyControllerTest {

    @Mock
    private IOfficeSupplyService supplyService;

    @Mock
    private IOfficeSupplyRequestService requestService;

    @Mock
    private IApprovalFlowLogService flowLogService;

    private AppSupplyController controller;

    /**
     * 初始化 MyBatis-Plus 实体表信息缓存，使控制器内构建的 LambdaUpdateWrapper 在纯 Mockito 环境下可用
     */
    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeSupplyRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeSupply.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppSupplyController(supplyService, requestService, flowLogService);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射注入当前登录用户（AuthInterceptor 的 ThreadLocal 无公开 setter） */
    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("撤销成功：原子条件更新 + 流程留痕")
    void revokeRequest_success_conditionalUpdateAndFlowLog() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        OfficeSupplyRequest request = new OfficeSupplyRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("王五");
        request.setStatus("0");
        when(requestService.getById(1L)).thenReturn(request);
        when(requestService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        var result = controller.revokeRequest(1L);

        assertEquals(200, result.getCode());
        // 用条件更新而非整单覆写，避免并发审批被覆盖
        verify(requestService).update(any(LambdaUpdateWrapper.class));
        verify(requestService, never()).updateById(any(OfficeSupplyRequest.class));
        // 成功必须写审批流程留痕，状态 3=已撤销
        ArgumentCaptor<String> bizType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(bizType.capture(), any(), any(), status.capture(), any(), any(), any());
        assertEquals("supply", bizType.getValue());
        assertEquals("3", status.getValue());
    }

    @Test
    @DisplayName("撤销失败（状态已变更）返回业务错误且写失败留痕（含实际状态）")
    void revokeRequest_conditionalUpdateFailed_flowLogWithActualStatus() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        OfficeSupplyRequest request = new OfficeSupplyRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("王五");
        request.setStatus("1"); // 模拟并发审批已把状态改走
        when(requestService.getById(1L)).thenReturn(request);
        when(requestService.update(any(LambdaUpdateWrapper.class))).thenReturn(false);

        var result = controller.revokeRequest(1L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("仅待审批"));
        // 失败路径也必须留痕：节点=撤销失败，状态=单据当前实际状态
        ArgumentCaptor<String> bizType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> node = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(bizType.capture(), any(), node.capture(), status.capture(), any(), any(), any());
        assertEquals("supply", bizType.getValue());
        assertEquals("撤销失败", node.getValue());
        assertEquals("1", status.getValue());
    }

    @Test
    @DisplayName("撤销申领接口声明 @OperLog 审计")
    void revokeRequest_hasOperLogAnnotation() throws Exception {
        var method = AppSupplyController.class.getMethod("revokeRequest", Long.class);
        assertNotNull(method.getAnnotation(com.linan.core.common.annotation.OperLog.class));
    }

    @Test
    @DisplayName("提交申领：伪造 userName/userId/status 被服务端强制覆盖")
    void submitRequest_forgedIdentity_overridden() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "supplyUser");
        OfficeSupply supply = new OfficeSupply();
        supply.setId(1L);
        supply.setSupplyName("A4纸");
        supply.setStock(100);
        when(supplyService.getById(1L)).thenReturn(supply);

        OfficeSupplyRequest request = new OfficeSupplyRequest();
        request.setSupplyId(1L);
        request.setQuantity(2);
        request.setUserId(999L);          // 伪造
        request.setUserName("伪造者");     // 伪造
        request.setStatus("2");           // 伪造
        when(requestService.save(any(OfficeSupplyRequest.class))).thenReturn(true);

        var result = controller.submitRequest(request);

        assertEquals(200, result.getCode());
        assertEquals(9L, request.getUserId(), "userId 必须为服务端当前登录用户");
        assertEquals("supplyUser", request.getUserName(), "userName 必须为服务端真实用户名");
        assertEquals("0", request.getStatus(), "status 必须强制为待审批");
        // flowLog 记录使用的 userName 是覆盖后的真实值
        ArgumentCaptor<String> operatorName = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(any(), any(), any(), any(), any(), operatorName.capture(), any());
        assertEquals("supplyUser", operatorName.getValue());
    }
}
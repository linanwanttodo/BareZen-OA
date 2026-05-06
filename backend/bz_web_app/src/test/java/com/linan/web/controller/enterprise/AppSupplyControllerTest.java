package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.ApprovalFlowFacade;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private ApprovalFlowFacade flowFacade;

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
        controller = new AppSupplyController(supplyService, requestService, flowFacade);
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
    @DisplayName("撤销成功：委托 Facade，handler 携带真实申请人并做原子条件更新")
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
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("supply"), eq(1L), eq(1L), eq("王五"), captor.capture());
        assertEquals(1L, captor.getValue().applicantId());
        // 用条件更新而非整单覆写，避免并发审批被覆盖
        assertTrue(captor.getValue().cancel());
        verify(requestService).update(any(LambdaUpdateWrapper.class));
        verify(requestService, never()).updateById(any(OfficeSupplyRequest.class));
    }

    @Test
    @DisplayName("撤销失败（状态已变更）：handler 携带实际状态供 Facade 失败留痕")
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

        assertEquals(200, result.getCode(), "归属校验/失败留痕由 Facade 收口，此处仅验证委托接线");
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("supply"), eq(1L), eq(1L), eq("王五"), captor.capture());
        assertEquals("1", captor.getValue().currentStatus(), "携带单据实际状态供撤销失败留痕");
        assertFalse(captor.getValue().cancel(), "状态已变更时条件更新失败");
        verify(requestService).update(any(LambdaUpdateWrapper.class));
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
        // 提交留痕与引擎启动由 Facade 统一编排，操作人为覆盖后的真实值
        verify(flowFacade).submit(eq("supply"), eq(request.getId()), eq(request.getAssigneeId()),
                eq(request.getAssigneeName()), eq(9L), eq("supplyUser"));
    }
}
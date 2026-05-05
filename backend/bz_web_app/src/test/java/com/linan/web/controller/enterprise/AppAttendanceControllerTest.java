package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.hr.service.IHrWorkLogService;
import com.linan.system.service.ApprovalFlowEngineService;
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
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppAttendanceController 撤销/提交接口单元测试
 *
 * <p>覆盖 T4：撤销加班/出差申请改为原子条件更新（与审批并发先到先得），
 * 撤销成功写流程留痕，撤销失败（状态已变更）也必须写 flow_log 留痕（含实际状态）；</p>
 * <p>覆盖 T6：提交接口 userName/userId/status 服务端强制覆盖，不信任请求体身份字段。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppAttendanceControllerTest {

    @Mock
    private IAttOvertimeRequestService overtimeService;

    @Mock
    private IAttTravelRequestService travelService;

    @Mock
    private IHrWorkLogService workLogService;

    @Mock
    private IApprovalFlowLogService flowLogService;

    @Mock
    private ApprovalFlowEngineService flowEngine;

    private AppAttendanceController controller;

    /**
     * 初始化 MyBatis-Plus 实体表信息缓存，使控制器内构建的 LambdaUpdateWrapper 在纯 Mockito 环境下可用
     */
    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttOvertimeRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttTravelRequest.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppAttendanceController(overtimeService, travelService, workLogService, flowLogService, flowEngine);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射注入当前登录用户（AuthInterceptor 的 ThreadLocal 无公开 setter） */
    private static void setCurrentUser(Long userId) throws Exception {
        setThreadLocal("currentUserIdHolder", userId);
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("未登录撤销加班申请返回请先登录")
    void revokeOvertime_notLoggedIn_returnsLoginRequired() {
        assertFalse(controller.revokeOvertime(1L).getMsg().isEmpty());
        verify(overtimeService, never()).update(any());
    }

    @Test
    @DisplayName("撤销他人加班申请被拒绝")
    void revokeOvertime_notOwner_rejected() throws Exception {
        setCurrentUser(2L);
        AttOvertimeRequest request = new AttOvertimeRequest();
        request.setId(1L);
        request.setUserId(1L);
        when(overtimeService.getById(1L)).thenReturn(request);

        var result = controller.revokeOvertime(1L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("只能撤销自己"));
        verify(overtimeService, never()).update(any());
    }

    @Test
    @DisplayName("撤销成功：原子条件更新 + 流程留痕")
    void revokeOvertime_success_conditionalUpdateAndFlowLog() throws Exception {
        setCurrentUser(1L);
        AttOvertimeRequest request = new AttOvertimeRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("张三");
        request.setStatus("0");
        when(overtimeService.getById(1L)).thenReturn(request);
        when(overtimeService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        var result = controller.revokeOvertime(1L);

        assertEquals(200, result.getCode());
        // 用条件更新（update wrapper）而非整单覆写（updateById），避免并发审批被覆盖
        verify(overtimeService).update(any(LambdaUpdateWrapper.class));
        verify(overtimeService, never()).updateById(any(AttOvertimeRequest.class));
        // 撤销成功必须写审批流程留痕，状态 3=已撤销
        ArgumentCaptor<String> bizType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> node = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(bizType.capture(), any(), node.capture(), status.capture(), any(), any(), any());
        assertEquals("overtime", bizType.getValue());
        assertEquals("撤销申请", node.getValue());
        assertEquals("3", status.getValue());
    }

    @Test
    @DisplayName("撤销失败（状态已变更）返回业务错误且写失败留痕（含实际状态）")
    void revokeOvertime_conditionalUpdateFailed_flowLogWithActualStatus() throws Exception {
        setCurrentUser(1L);
        AttOvertimeRequest request = new AttOvertimeRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("张三");
        request.setStatus("1"); // 模拟并发审批已把状态改走（实际状态非待审批）
        when(overtimeService.getById(1L)).thenReturn(request);
        // 条件更新命中 0 行 → 撤销失败
        when(overtimeService.update(any(LambdaUpdateWrapper.class))).thenReturn(false);

        var result = controller.revokeOvertime(1L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("仅待审批"));
        // 失败路径也必须留痕：节点=撤销失败，状态=单据当前实际状态
        ArgumentCaptor<String> bizType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> node = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(bizType.capture(), any(), node.capture(), status.capture(), any(), any(), any());
        assertEquals("overtime", bizType.getValue());
        assertEquals("撤销失败", node.getValue());
        assertEquals("1", status.getValue());
    }

    @Test
    @DisplayName("撤销出差申请同样使用原子条件更新且失败留痕")
    void revokeTravel_success_conditionalUpdate() throws Exception {
        setCurrentUser(1L);
        AttTravelRequest request = new AttTravelRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("李四");
        when(travelService.getById(1L)).thenReturn(request);
        when(travelService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        var result = controller.revokeTravel(1L);

        assertEquals(200, result.getCode());
        verify(travelService).update(any(LambdaUpdateWrapper.class));
        verify(travelService, never()).updateById(any(AttTravelRequest.class));
        // 撤销结果状态 3
        ArgumentCaptor<String> status = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(any(), any(), any(), status.capture(), any(), any(), any());
        assertEquals("3", status.getValue());
    }

    @Test
    @DisplayName("撤销接口声明 @OperLog 审计（businessType=3 删除）")
    void revokeMethods_haveOperLogAnnotation() throws Exception {
        Method revokeOvertime = AppAttendanceController.class.getMethod("revokeOvertime", Long.class);
        Method revokeTravel = AppAttendanceController.class.getMethod("revokeTravel", Long.class);
        assertNotNull(revokeOvertime.getAnnotation(com.linan.core.common.annotation.OperLog.class));
        assertNotNull(revokeTravel.getAnnotation(com.linan.core.common.annotation.OperLog.class));
    }

    @Test
    @DisplayName("提交加班申请：伪造 userName 被服务端强制覆盖")
    void submitOvertime_forgedIdentity_overridden() throws Exception {
        setThreadLocal("currentUserIdHolder", 7L);
        setThreadLocal("currentUsernameHolder", "realUser");
        AttOvertimeRequest request = new AttOvertimeRequest();
        request.setStartTime("2026-08-10 09:00");
        request.setEndTime("2026-08-10 18:00");
        request.setUserId(999L);          // 伪造
        request.setUserName("伪造者");     // 伪造
        request.setStatus("2");           // 伪造
        when(overtimeService.save(any(AttOvertimeRequest.class))).thenReturn(true);

        var result = controller.submitOvertime(request);

        assertEquals(200, result.getCode());
        assertEquals(7L, request.getUserId(), "userId 必须为服务端当前登录用户");
        assertEquals("realUser", request.getUserName(), "userName 必须为服务端真实用户名");
        assertEquals("0", request.getStatus(), "status 必须强制为待审批");
        // flowLog 记录使用的 userName 是覆盖后的真实值
        ArgumentCaptor<String> operatorName = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(any(), any(), any(), any(), any(), operatorName.capture(), any());
        assertEquals("realUser", operatorName.getValue());
    }

    @Test
    @DisplayName("提交出差申请：伪造 userName 被服务端强制覆盖")
    void submitTravel_forgedIdentity_overridden() throws Exception {
        setThreadLocal("currentUserIdHolder", 8L);
        setThreadLocal("currentUsernameHolder", "travelUser");
        AttTravelRequest request = new AttTravelRequest();
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        request.setUserId(999L);
        request.setUserName("伪造者");
        request.setStatus("1");
        when(travelService.save(any(AttTravelRequest.class))).thenReturn(true);

        var result = controller.submitTravel(request);

        assertEquals(200, result.getCode());
        assertEquals(8L, request.getUserId());
        assertEquals("travelUser", request.getUserName());
        assertEquals("0", request.getStatus());
        ArgumentCaptor<String> operatorName = ArgumentCaptor.forClass(String.class);
        verify(flowLogService).record(any(), any(), any(), any(), any(), operatorName.capture(), any());
        assertEquals("travelUser", operatorName.getValue());
    }
}
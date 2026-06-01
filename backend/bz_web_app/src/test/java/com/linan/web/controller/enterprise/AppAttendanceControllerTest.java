package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.hr.service.IHrWorkLogService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppAttendanceController 撤销/提交接口单元测试
 *
 * <p>审批编排已收口到 {@link ApprovalFlowFacade}：撤销的归属校验/引擎节点取消/
 * 流程留痕、提交的引擎启动/提交留痕均在 Facade 中（见 ApprovalFlowFacadeImplTest）。</p>
 *
 * <p>覆盖 T6：提交接口 userName/userId/status 服务端强制覆盖，不信任请求体身份字段。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppAttendanceControllerTest {

    @Mock
    private IAttOvertimeRequestService overtimeService;

    @Mock
    private IAttTravelRequestService travelService;

    @Mock
    private IAttLeaveRequestService leaveService;

    @Mock
    private IHrWorkLogService workLogService;

    @Mock
    private ApprovalFlowFacade flowFacade;

    private AppAttendanceController controller;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttOvertimeRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttTravelRequest.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttLeaveRequest.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppAttendanceController(overtimeService, travelService, leaveService,
                workLogService, flowFacade);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

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
        verify(flowFacade, never()).revoke(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("撤销加班申请：委托 Facade，handler 携带真实申请人并做原子条件更新")
    void revokeOvertime_delegatesToFacade() throws Exception {
        setCurrentUser(1L);
        AttOvertimeRequest request = new AttOvertimeRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("张三");
        request.setStatus(ApprovalStatus.PENDING.getValue());
        when(overtimeService.getById(1L)).thenReturn(request);
        when(overtimeService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        var result = controller.revokeOvertime(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("overtime"), eq(1L), eq(1L), eq("张三"), captor.capture());
        assertEquals(1L, captor.getValue().applicantId());
        // 原子条件更新（update wrapper）而非整单覆写（updateById）
        assertTrue(captor.getValue().cancel());
        verify(overtimeService).update(any(LambdaUpdateWrapper.class));
        verify(overtimeService, never()).updateById(any(AttOvertimeRequest.class));
        assertEquals(ApprovalStatus.PENDING.getValue(), captor.getValue().currentStatus());
    }

    @Test
    @DisplayName("撤销出差申请同样委托 Facade 使用原子条件更新")
    void revokeTravel_delegatesToFacade() throws Exception {
        setCurrentUser(1L);
        AttTravelRequest request = new AttTravelRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("李四");
        request.setStatus(ApprovalStatus.PENDING.getValue());
        when(travelService.getById(1L)).thenReturn(request);
        when(travelService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        var result = controller.revokeTravel(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("travel"), eq(1L), eq(1L), eq("李四"), captor.capture());
        assertTrue(captor.getValue().cancel());
        verify(travelService).update(any(LambdaUpdateWrapper.class));
        verify(travelService, never()).updateById(any(AttTravelRequest.class));
    }

    @Test
    @DisplayName("撤销接口声明 @OperLog 审计（businessType=3 删除）")
    void revokeMethods_haveOperLogAnnotation() throws Exception {
        var m = AppAttendanceController.class.getMethod("revokeOvertime", Long.class);
        var t = AppAttendanceController.class.getMethod("revokeTravel", Long.class);
        assertNotNull(m.getAnnotation(com.linan.core.common.annotation.OperLog.class));
        assertNotNull(t.getAnnotation(com.linan.core.common.annotation.OperLog.class));
    }

    @Test
    @DisplayName("提交加班申请：伪造 userName 被服务端强制覆盖并委托 Facade")
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
        assertEquals(ApprovalStatus.PENDING.getValue(), request.getStatus(), "status 必须强制为待审批");
        // 提交留痕操作符为真实用户名，由 Facade 统一编排
        verify(flowFacade).submit(eq("overtime"), eq(request.getId()), eq(request.getAssigneeId()),
                eq(request.getAssigneeName()), eq(7L), eq("realUser"));
    }

    @Test
    @DisplayName("提交出差申请：伪造 userName 被服务端强制覆盖并委托 Facade")
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
        assertEquals(ApprovalStatus.PENDING.getValue(), request.getStatus());
        verify(flowFacade).submit(eq("travel"), eq(request.getId()), eq(request.getAssigneeId()),
                eq(request.getAssigneeName()), eq(8L), eq("travelUser"));
    }

    // ==================== 请假申请 ====================

    @Test
    @DisplayName("提交请假申请：伪造身份字段被服务端强制覆盖并委托 Facade")
    void submitLeave_forgedIdentity_overridden() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "leaveUser");
        AttLeaveRequest request = new AttLeaveRequest();
        request.setLeaveType("annual");
        request.setStartTime("2026-08-12 09:00");
        request.setEndTime("2026-08-12 18:00");
        request.setUserId(999L);
        request.setUserName("伪造者");
        request.setStatus("2");
        when(leaveService.save(any(AttLeaveRequest.class))).thenReturn(true);

        var result = controller.submitLeave(request);

        assertEquals(200, result.getCode());
        assertEquals(9L, request.getUserId(), "userId 必须为服务端当前登录用户");
        assertEquals("leaveUser", request.getUserName(), "userName 必须为服务端真实用户名");
        assertEquals(ApprovalStatus.PENDING.getValue(), request.getStatus(), "status 必须强制为待审批");
        verify(flowFacade).submit(eq("leave"), eq(request.getId()), eq(request.getAssigneeId()),
                eq(request.getAssigneeName()), eq(9L), eq("leaveUser"));
    }

    @Test
    @DisplayName("提交请假申请：缺少请假类型被拒绝")
    void submitLeave_missingLeaveType_rejected() throws Exception {
        setCurrentUser(9L);
        AttLeaveRequest request = new AttLeaveRequest();
        request.setStartTime("2026-08-12 09:00");
        request.setEndTime("2026-08-12 18:00");

        var result = controller.submitLeave(request);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("请假类型"));
        verify(leaveService, never()).save(any());
    }

    @Test
    @DisplayName("撤销请假申请：委托 Facade，handler 携带申请人并做原子条件更新")
    void revokeLeave_delegatesToFacade() throws Exception {
        setCurrentUser(1L);
        AttLeaveRequest request = new AttLeaveRequest();
        request.setId(1L);
        request.setUserId(1L);
        request.setUserName("张三");
        request.setStatus(ApprovalStatus.PENDING.getValue());
        when(leaveService.getById(1L)).thenReturn(request);
        when(leaveService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        var result = controller.revokeLeave(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("leave"), eq(1L), eq(1L), eq("张三"), captor.capture());
        assertEquals(1L, captor.getValue().applicantId());
        assertTrue(captor.getValue().cancel());
        verify(leaveService).update(any(LambdaUpdateWrapper.class));
        verify(leaveService, never()).updateById(any(AttLeaveRequest.class));
        assertEquals(ApprovalStatus.PENDING.getValue(), captor.getValue().currentStatus());
    }

    // ==================== T1.3：审批字段清理与流程失败传播 ====================

    @Test
    @DisplayName("提交加班申请：预置 id/审批字段被服务端强制清空")
    void submitOvertime_forgedApprovalFields_cleared() throws Exception {
        setThreadLocal("currentUserIdHolder", 7L);
        setThreadLocal("currentUsernameHolder", "realUser");
        AttOvertimeRequest request = new AttOvertimeRequest();
        request.setStartTime("2026-08-10 09:00");
        request.setEndTime("2026-08-10 18:00");
        request.setId(12345L);                 // 预置主键
        request.setApproverId(66L);            // 预置审批人
        request.setApproverName("审批人");
        request.setApproveTime("2026-08-10 19:00");
        request.setApproveRemark("已同意");
        when(overtimeService.save(any(AttOvertimeRequest.class))).thenReturn(true);

        var result = controller.submitOvertime(request);

        assertEquals(200, result.getCode());
        assertNull(request.getId(), "id 必须被清空（防主键冲突/覆盖）");
        assertNull(request.getApproverId(), "approverId 必须被清空");
        assertNull(request.getApproverName(), "approverName 必须被清空");
        assertNull(request.getApproveTime(), "approveTime 必须被清空");
        assertNull(request.getApproveRemark(), "approveRemark 必须被清空");
    }

    @Test
    @DisplayName("提交出差申请：预置 id/审批字段被服务端强制清空")
    void submitTravel_forgedApprovalFields_cleared() throws Exception {
        setThreadLocal("currentUserIdHolder", 8L);
        setThreadLocal("currentUsernameHolder", "travelUser");
        AttTravelRequest request = new AttTravelRequest();
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        request.setId(12345L);
        request.setApproverId(66L);
        request.setApproverName("审批人");
        request.setApproveTime("2026-08-11 19:00");
        request.setApproveRemark("已同意");
        when(travelService.save(any(AttTravelRequest.class))).thenReturn(true);

        var result = controller.submitTravel(request);

        assertEquals(200, result.getCode());
        assertNull(request.getId());
        assertNull(request.getApproverId());
        assertNull(request.getApproverName());
        assertNull(request.getApproveTime());
        assertNull(request.getApproveRemark());
    }

    @Test
    @DisplayName("提交加班申请：审批流程失败时必须抛异常（触发事务回滚）")
    void submitOvertime_flowFailure_propagatesException() throws Exception {
        setThreadLocal("currentUserIdHolder", 7L);
        setThreadLocal("currentUsernameHolder", "realUser");
        AttOvertimeRequest request = new AttOvertimeRequest();
        request.setStartTime("2026-08-10 09:00");
        request.setEndTime("2026-08-10 18:00");
        when(overtimeService.save(any(AttOvertimeRequest.class))).thenReturn(true);
        doThrow(new IllegalStateException("审批引擎不可用"))
                .when(flowFacade).submit(anyString(), any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> controller.submitOvertime(request),
                "流程失败必须向上抛异常，事务才能回滚已保存的单据");
        verify(flowFacade).submit(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("提交出差申请：审批流程失败时必须抛异常（触发事务回滚）")
    void submitTravel_flowFailure_propagatesException() throws Exception {
        setThreadLocal("currentUserIdHolder", 8L);
        setThreadLocal("currentUsernameHolder", "travelUser");
        AttTravelRequest request = new AttTravelRequest();
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        when(travelService.save(any(AttTravelRequest.class))).thenReturn(true);
        doThrow(new IllegalStateException("审批引擎不可用"))
                .when(flowFacade).submit(anyString(), any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> controller.submitTravel(request),
                "流程失败必须向上抛异常，事务才能回滚已保存的单据");
    }

    @Test
    @DisplayName("提交加班/出差方法声明 @Transactional（事务一致性）")
    void submitMethods_haveTransactionalAnnotation() throws Exception {
        assertNotNull(AppAttendanceController.class
                .getMethod("submitOvertime", AttOvertimeRequest.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertNotNull(AppAttendanceController.class
                .getMethod("submitTravel", AttTravelRequest.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }
}
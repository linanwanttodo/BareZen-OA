package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeVehicleRequestService;
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
 * AppVehicleController 撤销/提交/详情接口单元测试
 *
 * <p>对齐 AppSupplyControllerTest：提交身份字段服务端强制覆盖、审批字段清空、
 * 流程失败传播触发事务回滚；撤销走 Facade 委托 + 原子条件更新。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppVehicleControllerTest {

    @Mock
    private IOfficeVehicleRequestService vehicleService;

    @Mock
    private ApprovalFlowFacade flowFacade;

    private AppVehicleController controller;

    /**
     * 初始化 MyBatis-Plus 实体表信息缓存，使控制器内构建的 LambdaUpdateWrapper 在纯 Mockito 环境下可用
     */
    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeVehicleRequest.class);
    }

    @BeforeEach
    void setUp() {
        controller = new AppVehicleController(vehicleService, flowFacade);
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
    @DisplayName("提交用车：伪造 applicantId/applicantName/status 被服务端强制覆盖")
    void submit_forgedIdentity_overridden() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "vehicleUser");

        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setPurpose("接送客户");
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        request.setApplicantId(999L);      // 伪造
        request.setApplicantName("伪造者"); // 伪造
        request.setStatus("2");            // 伪造
        when(vehicleService.save(any(OfficeVehicleRequest.class))).thenReturn(true);

        var result = controller.submit(request);

        assertEquals(200, result.getCode());
        assertEquals(9L, request.getApplicantId(), "applicantId 必须为服务端当前登录用户");
        assertEquals("vehicleUser", request.getApplicantName(), "applicantName 必须为服务端真实用户名");
        assertEquals("0", request.getStatus(), "status 必须强制为待审批");
        verify(flowFacade).submit(eq("vehicle"), eq(request.getId()), eq(request.getAssigneeId()),
                eq(request.getAssigneeName()), eq(9L), eq("vehicleUser"));
    }

    @Test
    @DisplayName("提交用车：预置 id/审批字段被服务端强制清空")
    void submit_forgedApprovalFields_cleared() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "vehicleUser");

        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setPurpose("接送客户");
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        request.setId(54321L);                 // 预置主键
        request.setApproverId(66L);            // 预置审批人
        request.setApproverName("审批人");
        request.setApproveTime("2026-08-10 19:00");
        request.setApproveRemark("已同意");
        when(vehicleService.save(any(OfficeVehicleRequest.class))).thenReturn(true);

        var result = controller.submit(request);

        assertEquals(200, result.getCode());
        assertNull(request.getId(), "id 必须被清空（防主键冲突/覆盖）");
        assertNull(request.getApproverId(), "approverId 必须被清空");
        assertNull(request.getApproverName(), "approverName 必须被清空");
        assertNull(request.getApproveTime(), "approveTime 必须被清空");
        assertNull(request.getApproveRemark(), "approveRemark 必须被清空");
    }

    @Test
    @DisplayName("提交用车：起止时间缺失或起晚于止返回错误，不落库不触达 Facade")
    void submit_invalidTime_rejected() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);

        OfficeVehicleRequest missing = new OfficeVehicleRequest();
        missing.setPurpose("接送客户");
        missing.setStartTime("2026-08-11 09:00");
        var missingResult = controller.submit(missing);
        assertEquals(500, missingResult.getCode());
        assertEquals("请选择用车时间", missingResult.getMsg());

        OfficeVehicleRequest reversed = new OfficeVehicleRequest();
        reversed.setPurpose("接送客户");
        reversed.setStartTime("2026-08-11 18:00");
        reversed.setEndTime("2026-08-11 09:00");
        var reversedResult = controller.submit(reversed);
        assertEquals(500, reversedResult.getCode());
        assertEquals("结束时间必须晚于开始时间", reversedResult.getMsg());

        verify(vehicleService, never()).save(any(OfficeVehicleRequest.class));
        verify(flowFacade, never()).submit(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("提交用车：审批流程失败时必须抛异常（触发事务回滚）")
    void submit_flowFailure_propagatesException() throws Exception {
        setThreadLocal("currentUserIdHolder", 9L);
        setThreadLocal("currentUsernameHolder", "vehicleUser");
        when(vehicleService.save(any(OfficeVehicleRequest.class))).thenReturn(true);
        doThrow(new IllegalStateException("审批引擎不可用"))
                .when(flowFacade).submit(anyString(), any(), any(), any(), any(), any());

        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setPurpose("接送客户");
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        assertThrows(RuntimeException.class, () -> controller.submit(request),
                "流程失败必须向上抛异常，事务才能回滚已保存的单据");
    }

    @Test
    @DisplayName("提交用车方法声明 @Transactional（事务一致性）")
    void submit_hasTransactionalAnnotation() throws Exception {
        assertNotNull(AppVehicleController.class
                .getMethod("submit", OfficeVehicleRequest.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    @DisplayName("撤销成功：委托 Facade，handler 携带真实申请人并做原子条件更新")
    void revokeRequest_success_conditionalUpdateAndFlowLog() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(1L);
        request.setApplicantId(1L);
        request.setApplicantName("王五");
        request.setStatus(ApprovalStatus.PENDING.getValue());
        when(vehicleService.getById(1L)).thenReturn(request);
        when(vehicleService.update(any(LambdaUpdateWrapper.class))).thenReturn(true);

        var result = controller.revokeRequest(1L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("vehicle"), eq(1L), eq(1L), eq("王五"), captor.capture());
        assertEquals(1L, captor.getValue().applicantId());
        // 用条件更新而非整单覆写，避免并发审批被覆盖
        assertTrue(captor.getValue().cancel());
        verify(vehicleService).update(any(LambdaUpdateWrapper.class));
        verify(vehicleService, never()).updateById(any(OfficeVehicleRequest.class));
    }

    @Test
    @DisplayName("撤销失败（状态已变更）：handler 携带实际状态供 Facade 失败留痕")
    void revokeRequest_conditionalUpdateFailed_flowLogWithActualStatus() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(1L);
        request.setApplicantId(1L);
        request.setApplicantName("王五");
        request.setStatus(ApprovalStatus.APPROVED.getValue()); // 模拟并发审批已把状态改走
        when(vehicleService.getById(1L)).thenReturn(request);
        when(vehicleService.update(any(LambdaUpdateWrapper.class))).thenReturn(false);

        var result = controller.revokeRequest(1L);

        assertEquals(200, result.getCode(), "归属校验/失败留痕由 Facade 收口，此处仅验证委托接线");
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("vehicle"), eq(1L), eq(1L), eq("王五"), captor.capture());
        assertEquals(ApprovalStatus.APPROVED.getValue(), captor.getValue().currentStatus(),
                "携带单据实际状态供撤销失败留痕");
        assertFalse(captor.getValue().cancel(), "状态已变更时条件更新失败");
        verify(vehicleService).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("撤销用车接口声明 @OperLog 审计")
    void revokeRequest_hasOperLogAnnotation() throws Exception {
        var method = AppVehicleController.class.getMethod("revokeRequest", Long.class);
        assertNotNull(method.getAnnotation(com.linan.core.common.annotation.OperLog.class));
    }

    @Test
    @DisplayName("详情：本人可查；非本人被拒绝，不存在返回错误")
    void detail_ownershipEnforced() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);

        OfficeVehicleRequest mine = new OfficeVehicleRequest();
        mine.setId(10L);
        mine.setApplicantId(1L);
        mine.setDeparture("公司总部");
        mine.setDestination("高铁站");
        when(vehicleService.getById(10L)).thenReturn(mine);

        var ownResult = controller.detail(10L);
        assertEquals(200, ownResult.getCode());
        assertEquals("公司总部", ownResult.getData().getDeparture());

        OfficeVehicleRequest others = new OfficeVehicleRequest();
        others.setId(11L);
        others.setApplicantId(2L);
        when(vehicleService.getById(11L)).thenReturn(others);
        assertEquals(500, controller.detail(11L).getCode(), "非本人申请不可查看");

        when(vehicleService.getById(12L)).thenReturn(null);
        assertEquals(500, controller.detail(12L).getCode(), "记录不存在返回错误");
    }

    @Test
    @DisplayName("我的用车列表：未登录返回错误")
    void myRequests_notLogin_rejected() {
        var result = controller.myRequests(1, 10, null);

        assertEquals(500, result.getCode());
        assertEquals("请先登录", result.getMsg());
    }
}

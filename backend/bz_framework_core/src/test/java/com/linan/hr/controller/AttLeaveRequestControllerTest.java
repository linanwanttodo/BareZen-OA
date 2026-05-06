package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.linan.core.common.api.Result;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.system.service.ApprovalFlowFacade;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 请假审批 Controller 单元测试
 *
 * <p>审批编排已收口到 {@link ApprovalFlowFacade}，本测试聚焦 Controller 的
 * 委托接线（submitted/approve/revoke 均传入单据原子更新 handler）与编排外校验，
 * 引擎推进/流程日志/通知事件的断言迁移至 ApprovalFlowFacadeImplTest。</p>
 */
@ExtendWith(MockitoExtension.class)
class AttLeaveRequestControllerTest {

    @Mock
    private IAttLeaveRequestService leaveService;

    @Mock
    private ApprovalFlowFacade flowFacade;

    @InjectMocks
    private AttLeaveRequestController controller;

    private Long approverId;
    private Long applicantId;

    @BeforeAll
    static void initTableInfoCache() {
        // LambdaUpdateWrapper 在纯 Mockito 环境下需要实体列映射
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), AttLeaveRequest.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        approverId = 1L;
        applicantId = 2L;
        setThreadLocal("currentUserIdHolder", approverId);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() throws Exception {
        AuthInterceptor.clearCurrentUser();
    }

    private void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("审批：委托 Facade，handler 携带申请人/终态原子更新/通知文案")
    void approve_delegatesToFacade() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(10L);
        leave.setUserId(applicantId);
        leave.setStartTime("2026-08-10 09:00");
        leave.setEndTime("2026-08-10 18:00");
        leave.setStatus(ApprovalStatus.PENDING.getValue());
        when(leaveService.getById(10L)).thenReturn(leave);

        Result<Void> result = controller.approve(10L, "pass", "同意");

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> h =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).approve(eq("leave"), eq(10L), eq(approverId), eq("admin"),
                eq("pass"), eq("同意"), h.capture());
        assertEquals(applicantId, h.getValue().applicantId(), "handler 携带真实申请人供 Facade 自审校验");
    }

    @Test
    @DisplayName("记录不存在返回错误，不触达 Facade")
    void approve_notFound_rejected() {
        when(leaveService.getById(10L)).thenReturn(null);

        Result<Void> result = controller.approve(10L, "pass", null);

        assertEquals(500, result.getCode());
        verify(flowFacade, never()).approve(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("撤销：仅本人可撤销，委托 Facade 的撤销 handler 做原子更新")
    void revoke_delegatesToFacade() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(15L);
        leave.setUserId(approverId); // 当前登录用户即申请人
        leave.setUserName("admin");
        leave.setStatus(ApprovalStatus.PENDING.getValue());
        when(leaveService.getById(15L)).thenReturn(leave);
        when(leaveService.update(any())).thenReturn(true);

        Result<Void> result = controller.revoke(15L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("leave"), eq(15L), eq(approverId), eq("admin"), captor.capture());

        // handler.cancel 做原子条件更新：仅待审批→撤销
        boolean ok = captor.getValue().cancel();
        assertTrue(ok);
        LambdaUpdateWrapper<AttLeaveRequest> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getSqlSegment().contains("status"));
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        assertTrue(params.containsValue(ApprovalStatus.CANCELED.getValue()));
    }

    @Test
    @DisplayName("撤销他人申请：handler 携带真实申请人，由 Facade 拒绝（Controller 不越权判定）")
    void revoke_otherUsersApplication_delegatedToFacade() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(15L);
        leave.setUserId(99L);
        leave.setStatus(ApprovalStatus.PENDING.getValue());
        when(leaveService.getById(15L)).thenReturn(leave);

        Result<Void> result = controller.revoke(15L);

        assertEquals(200, result.getCode(), "归属校验收口到 Facade，此处仅验证委托接线");
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("leave"), eq(15L), eq(approverId), eq("admin"), captor.capture());
        assertEquals(99L, captor.getValue().applicantId());
        verify(leaveService, never()).update(any());
    }

    @Test
    @DisplayName("提交申请：状态为待审批，申请人自动填充")
    void submit_setsStatusAndApplicant() {
        AttLeaveRequest request = new AttLeaveRequest();
        request.setStartTime("2026-08-10 09:00");
        request.setEndTime("2026-08-10 18:00");
        request.setUserName("张三");
        when(leaveService.save(any(AttLeaveRequest.class))).thenReturn(true);

        Result<Void> result = controller.submit(request);

        assertEquals(200, result.getCode());
        assertEquals(approverId, request.getUserId());
        assertEquals(ApprovalStatus.PENDING.getValue(), request.getStatus());
        verify(flowFacade).submit(eq("leave"), eq(request.getId()), eq(request.getAssigneeId()),
                eq(request.getAssigneeName()), eq(approverId), eq("张三"));
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<AttLeaveRequest> captureUpdateWrapper() {
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<AttLeaveRequest>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(leaveService).update(captor.capture());
        return (LambdaUpdateWrapper<AttLeaveRequest>) captor.getValue();
    }
}
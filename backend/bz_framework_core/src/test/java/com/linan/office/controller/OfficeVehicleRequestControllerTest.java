package com.linan.office.controller;

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
 * 用车申请 Controller 单元测试
 *
 * <p>审批编排已收口到 {@link ApprovalFlowFacade}，本测试聚焦 Controller 的
 * 委托接线（submit/approve/cancel 均传入单据原子更新 handler）与编排外校验，
 * 引擎推进/流程日志/通知事件断言迁移至 ApprovalFlowFacadeImplTest。</p>
 */
@ExtendWith(MockitoExtension.class)
class OfficeVehicleRequestControllerTest {

    @Mock
    private IOfficeVehicleRequestService vehicleService;

    @Mock
    private ApprovalFlowFacade flowFacade;

    @InjectMocks
    private OfficeVehicleRequestController controller;

    private Long approverId;
    private Long applicantId;

    @BeforeAll
    static void initTableInfoCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OfficeVehicleRequest.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        approverId = 1L;
        applicantId = 2L;
        setThreadLocal("currentUserIdHolder", approverId);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() {
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
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(20L);
        request.setApplicantId(applicantId);
        request.setPurpose("拜访客户");
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        request.setStatus(ApprovalStatus.PENDING.getValue());
        when(vehicleService.getById(20L)).thenReturn(request);

        Result<Void> result = controller.approve(20L, "pass", "同意出车");

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> h =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).approve(eq("vehicle"), eq(20L), eq(approverId), eq("admin"),
                eq("pass"), eq("同意出车"), h.capture());
        assertEquals(applicantId, h.getValue().applicantId(), "handler 携带真实申请人供 Facade 自审校验");
    }

    @Test
    @DisplayName("记录不存在返回错误，不触达 Facade")
    void approve_notFound_rejected() {
        when(vehicleService.getById(20L)).thenReturn(null);

        Result<Void> result = controller.approve(20L, "pass", null);

        assertEquals(500, result.getCode());
        verify(flowFacade, never()).approve(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("取消：handler 携带真实申请人，由 Facade 做归属校验；原子更新使用枚举状态")
    void cancel_delegatesToFacade() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(22L);
        request.setApplicantId(approverId); // 当前登录用户即申请人
        request.setStatus(ApprovalStatus.PENDING.getValue());
        when(vehicleService.getById(22L)).thenReturn(request);
        when(vehicleService.update(any())).thenReturn(true);

        Result<Void> result = controller.cancel(22L);

        assertEquals(200, result.getCode());
        ArgumentCaptor<ApprovalFlowFacade.ApprovalBizHandler> captor =
                ArgumentCaptor.forClass(ApprovalFlowFacade.ApprovalBizHandler.class);
        verify(flowFacade).revoke(eq("vehicle"), eq(22L), eq(approverId), eq("admin"), captor.capture());
        assertEquals(approverId, captor.getValue().applicantId());
        // handler.cancel 原子条件更新：仅待审批→已撤销
        boolean ok = captor.getValue().cancel();
        assertTrue(ok);
        LambdaUpdateWrapper<OfficeVehicleRequest> wrapper = captureUpdateWrapper();
        assertTrue(wrapper.getSqlSegment().contains("status"));
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        assertTrue(params.containsValue(ApprovalStatus.CANCELED.getValue()));
    }

    @Test
    @DisplayName("提交申请：状态为待审批，申请人自动填充并委托 Facade 发起流程")
    void submit_setsStatusAndApplicant() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setStartTime("2026-08-11 09:00");
        request.setEndTime("2026-08-11 18:00");
        request.setPurpose("拜访客户");
        request.setApplicantName("李四");
        when(vehicleService.save(any(OfficeVehicleRequest.class))).thenReturn(true);

        Result<Void> result = controller.submit(request);

        assertEquals(200, result.getCode());
        assertEquals(approverId, request.getApplicantId());
        assertEquals(ApprovalStatus.PENDING.getValue(), request.getStatus());
        verify(flowFacade).submit(eq("vehicle"), eq(request.getId()), eq(request.getAssigneeId()),
                eq(request.getAssigneeName()), eq(approverId), eq("李四"));
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<OfficeVehicleRequest> captureUpdateWrapper() {
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper<OfficeVehicleRequest>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(vehicleService).update(captor.capture());
        return (LambdaUpdateWrapper<OfficeVehicleRequest>) captor.getValue();
    }
}
package com.linan.office.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeVehicleRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用车申请 Controller 单元测试
 *
 * <p>覆盖本次审查修复的关键业务规则：禁止自审自批、审批人姓名使用真实用户名、取消限制。</p>
 */
@ExtendWith(MockitoExtension.class)
class OfficeVehicleRequestControllerTest {

    @Mock
    private IOfficeVehicleRequestService vehicleService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OfficeVehicleRequestController controller;

    private Long approverId;
    private Long applicantId;

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
    @DisplayName("审批通过：审批人姓名为当前用户名")
    void approve_pass_usesRealUsername() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(20L);
        request.setApplicantId(applicantId);
        request.setStatus("0");
        when(vehicleService.getById(20L)).thenReturn(request);
        when(vehicleService.updateById(any(OfficeVehicleRequest.class))).thenReturn(true);

        Result<Void> result = controller.approve(20L, "pass", "同意出车");

        assertEquals(200, result.getCode());
        assertEquals("1", request.getStatus());
        assertEquals(approverId, request.getApproverId());
        assertEquals("admin", request.getApproverName());
        assertNotNull(request.getApproveTime());
    }

    @Test
    @DisplayName("禁止审批自己的用车申请")
    void approve_ownApplication_rejected() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(21L);
        request.setApplicantId(approverId);
        request.setStatus("0");
        when(vehicleService.getById(21L)).thenReturn(request);

        Result<Void> result = controller.approve(21L, "pass", null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不能审批自己的申请"));
        verify(vehicleService, never()).updateById(any());
    }

    @Test
    @DisplayName("取消：仅本人可取消自己的申请")
    void cancel_otherUsersApplication_rejected() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(22L);
        request.setApplicantId(99L);
        request.setStatus("0");
        when(vehicleService.getById(22L)).thenReturn(request);

        Result<Void> result = controller.cancel(22L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("只能取消自己的申请"));
        verify(vehicleService, never()).updateById(any());
    }

    @Test
    @DisplayName("已审批的申请不能重复审批")
    void approve_alreadyApproved_rejected() {
        OfficeVehicleRequest request = new OfficeVehicleRequest();
        request.setId(23L);
        request.setApplicantId(applicantId);
        request.setStatus("1");
        when(vehicleService.getById(23L)).thenReturn(request);

        Result<Void> result = controller.approve(23L, "pass", null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("已审批"));
    }

    @Test
    @DisplayName("提交申请：状态为待审批，申请人自动填充")
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
        assertEquals("0", request.getStatus());
    }
}

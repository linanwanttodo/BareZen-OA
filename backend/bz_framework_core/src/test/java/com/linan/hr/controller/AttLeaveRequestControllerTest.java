package com.linan.hr.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
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
 * 请假审批 Controller 单元测试
 *
 * <p>覆盖本次审查修复的关键业务规则：禁止自审自批、审批人姓名使用真实用户名。</p>
 */
@ExtendWith(MockitoExtension.class)
class AttLeaveRequestControllerTest {

    @Mock
    private IAttLeaveRequestService leaveService;

    @InjectMocks
    private AttLeaveRequestController controller;

    private Long approverId;
    private Long applicantId;

    @BeforeEach
    void setUp() throws Exception {
        approverId = 1L;
        applicantId = 2L;

        // 通过反射设置 ThreadLocal 模拟当前登录用户（审批人）
        setThreadLocal("currentUserIdHolder", approverId);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() throws Exception {
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射设置 AuthInterceptor 的私有 ThreadLocal */
    private void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("审批通过：状态置为已通过，审批人姓名为当前用户名")
    void approve_pass_usesRealUsername() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(10L);
        leave.setUserId(applicantId); // 申请人不是当前审批人
        leave.setStatus("0");
        when(leaveService.getById(10L)).thenReturn(leave);
        when(leaveService.updateById(any(AttLeaveRequest.class))).thenReturn(true);

        Result<Void> result = controller.approve(10L, "pass", "同意");

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("1", leave.getStatus());       // 已通过
        assertEquals(approverId, leave.getApproverId());
        assertEquals("admin", leave.getApproverName()); // 真实用户名而非 userId 数字
        assertNotNull(leave.getApproveTime());
        assertEquals("同意", leave.getApproveRemark());
    }

    @Test
    @DisplayName("禁止审批自己的申请")
    void approve_ownApplication_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(11L);
        leave.setUserId(approverId); // 申请人与审批人相同
        leave.setStatus("0");
        when(leaveService.getById(11L)).thenReturn(leave);

        Result<Void> result = controller.approve(11L, "pass", null);

        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不能审批自己的申请"));
        // 不允许更新
        verify(leaveService, never()).updateById(any());
    }

    @Test
    @DisplayName("重复审批被拒绝")
    void approve_alreadyApproved_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(12L);
        leave.setUserId(applicantId);
        leave.setStatus("1"); // 已审批
        when(leaveService.getById(12L)).thenReturn(leave);

        Result<Void> result = controller.approve(12L, "pass", null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("已审批"));
        verify(leaveService, never()).updateById(any());
    }

    @Test
    @DisplayName("驳回：状态置为已驳回")
    void approve_reject_setsStatus() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(13L);
        leave.setUserId(applicantId);
        leave.setStatus("0");
        when(leaveService.getById(13L)).thenReturn(leave);
        when(leaveService.updateById(any(AttLeaveRequest.class))).thenReturn(true);

        Result<Void> result = controller.approve(13L, "reject", "事由不充分");

        assertEquals(200, result.getCode());
        assertEquals("2", leave.getStatus());
        assertEquals("事由不充分", leave.getApproveRemark());
    }

    @Test
    @DisplayName("无效审批动作被拒绝")
    void approve_invalidAction_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(14L);
        leave.setUserId(applicantId);
        leave.setStatus("0");
        when(leaveService.getById(14L)).thenReturn(leave);

        Result<Void> result = controller.approve(14L, "unknown", null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("无效的审批动作"));
        verify(leaveService, never()).updateById(any());
    }

    @Test
    @DisplayName("撤销：仅本人可撤销自己的申请")
    void revoke_otherUsersApplication_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(15L);
        leave.setUserId(99L); // 不是当前用户
        leave.setStatus("0");
        when(leaveService.getById(15L)).thenReturn(leave);

        Result<Void> result = controller.revoke(15L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("只能撤销自己的申请"));
        verify(leaveService, never()).updateById(any());
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
        assertEquals("0", request.getStatus());
    }
}

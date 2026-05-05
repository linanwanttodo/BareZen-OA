package com.linan.hr.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 请假记录删除仅限待审批状态 单测
 *
 * <p>覆盖 T5：已流转单据（status!=0）拒绝删除且不触库；待审批单据放行；不存在返回错误；
 * @OperLog 与 @RequirePermission 保留。</p>
 */
@ExtendWith(MockitoExtension.class)
class AttLeaveRequestControllerDeleteTest {

    @Mock
    private IAttLeaveRequestService leaveService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.linan.system.service.IApprovalFlowLogService flowLogService;

    @InjectMocks
    private AttLeaveRequestController controller;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("status=1 已审批：拒绝删除且不 removeById")
    void delete_alreadyProcessed_rejected() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(1L);
        leave.setStatus("1");
        when(leaveService.getById(1L)).thenReturn(leave);

        Result<Void> result = controller.delete(1L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("仅待审批状态"));
        verify(leaveService, never()).removeById(1L);
    }

    @Test
    @DisplayName("status=0 待审批：放行删除")
    void delete_pending_allowed() {
        AttLeaveRequest leave = new AttLeaveRequest();
        leave.setId(2L);
        leave.setStatus("0");
        when(leaveService.getById(2L)).thenReturn(leave);
        when(leaveService.removeById(2L)).thenReturn(true);

        Result<Void> result = controller.delete(2L);

        assertEquals(200, result.getCode());
        verify(leaveService).removeById(2L);
    }

    @Test
    @DisplayName("记录不存在返回错误")
    void delete_notFound_rejected() {
        when(leaveService.getById(99L)).thenReturn(null);

        Result<Void> result = controller.delete(99L);

        assertEquals(500, result.getCode());
        verify(leaveService, never()).removeById(99L);
    }

    @Test
    @DisplayName("删除接口保留 @OperLog 与 @RequirePermission")
    void delete_annotationsRetained() throws Exception {
        var method = AttLeaveRequestController.class.getMethod("delete", Long.class);
        assertNotNull(method.getAnnotation(OperLog.class));
        RequirePermission rp = method.getAnnotation(RequirePermission.class);
        assertNotNull(rp, "删除接口必须保留 @RequirePermission");
        assertEquals("hr:leave:delete", rp.value()[0]);
    }
}
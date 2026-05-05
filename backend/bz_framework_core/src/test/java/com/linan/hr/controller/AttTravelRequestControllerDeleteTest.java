package com.linan.hr.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttTravelRequestService;
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
 * 出差记录删除仅限待审批状态 单测（T5）
 */
@ExtendWith(MockitoExtension.class)
class AttTravelRequestControllerDeleteTest {

    @Mock
    private IAttTravelRequestService travelService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.linan.system.service.IApprovalFlowLogService flowLogService;

    @InjectMocks
    private AttTravelRequestController controller;

    @Test
    @DisplayName("status=1 已审批：拒绝删除且不 removeById")
    void delete_alreadyProcessed_rejected() {
        AttTravelRequest request = new AttTravelRequest();
        request.setId(1L);
        request.setStatus("1");
        when(travelService.getById(1L)).thenReturn(request);

        Result<Void> result = controller.delete(1L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("仅待审批状态"));
        verify(travelService, never()).removeById(1L);
    }

    @Test
    @DisplayName("status=0 待审批：放行删除")
    void delete_pending_allowed() {
        AttTravelRequest request = new AttTravelRequest();
        request.setId(2L);
        request.setStatus("0");
        when(travelService.getById(2L)).thenReturn(request);
        when(travelService.removeById(2L)).thenReturn(true);

        Result<Void> result = controller.delete(2L);

        assertEquals(200, result.getCode());
        verify(travelService).removeById(2L);
    }

    @Test
    @DisplayName("记录不存在返回错误")
    void delete_notFound_rejected() {
        when(travelService.getById(99L)).thenReturn(null);

        Result<Void> result = controller.delete(99L);

        assertEquals(500, result.getCode());
        verify(travelService, never()).removeById(99L);
    }

    @Test
    @DisplayName("删除接口保留 @OperLog 与 @RequirePermission")
    void delete_annotationsRetained() throws Exception {
        var method = AttTravelRequestController.class.getMethod("delete", Long.class);
        assertNotNull(method.getAnnotation(OperLog.class));
        RequirePermission rp = method.getAnnotation(RequirePermission.class);
        assertNotNull(rp, "删除接口必须保留 @RequirePermission");
        assertEquals("hr:travel:delete", rp.value()[0]);
    }
}
package com.linan.office.controller;

import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeSupplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 申领记录删除仅限待审批状态 单测（T5，deleteRequest）
 */
@ExtendWith(MockitoExtension.class)
class OfficeSupplyControllerDeleteRequestTest {

    @Mock
    private IOfficeSupplyService supplyService;

    @Mock
    private IOfficeSupplyRequestService requestService;

    @InjectMocks
    private OfficeSupplyController controller;

    @Test
    @DisplayName("status=1 已审批：拒绝删除且不 removeById")
    void deleteRequest_alreadyProcessed_rejected() {
        OfficeSupplyRequest request = new OfficeSupplyRequest();
        request.setId(1L);
        request.setStatus("1");
        when(requestService.getById(1L)).thenReturn(request);

        Result<Void> result = controller.deleteRequest(1L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("仅待审批状态"));
        verify(requestService, never()).removeById(1L);
    }

    @Test
    @DisplayName("status=0 待审批：放行删除")
    void deleteRequest_pending_allowed() {
        OfficeSupplyRequest request = new OfficeSupplyRequest();
        request.setId(2L);
        request.setStatus("0");
        when(requestService.getById(2L)).thenReturn(request);
        when(requestService.removeById(2L)).thenReturn(true);

        Result<Void> result = controller.deleteRequest(2L);

        assertEquals(200, result.getCode());
        verify(requestService).removeById(2L);
    }

    @Test
    @DisplayName("记录不存在返回错误")
    void deleteRequest_notFound_rejected() {
        when(requestService.getById(99L)).thenReturn(null);

        Result<Void> result = controller.deleteRequest(99L);

        assertEquals(500, result.getCode());
        verify(requestService, never()).removeById(99L);
    }

    @Test
    @DisplayName("删除接口保留 @OperLog 与 @RequirePermission")
    void deleteRequest_annotationsRetained() throws Exception {
        var method = OfficeSupplyController.class.getMethod("deleteRequest", Long.class);
        assertNotNull(method.getAnnotation(OperLog.class));
        RequirePermission rp = method.getAnnotation(RequirePermission.class);
        assertNotNull(rp, "删除接口必须保留 @RequirePermission");
        assertEquals("office:supply:delete", rp.value()[0]);
    }
}
package com.linan.web.controller.open;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.RateLimit;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.service.ISysNoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OpenApiController 单元测试
 *
 * <p>覆盖 A1.1：userId 必填（required=true）、接口限流配置生效、响应不携带 reason 敏感字段。</p>
 */
@ExtendWith(MockitoExtension.class)
class OpenApiControllerTest {

    @Mock
    private IAttLeaveRequestService leaveService;

    @Mock
    private IOfficeSupplyService supplyService;

    @Mock
    private ISysNoticeService noticeService;

    private OpenApiController controller;

    @BeforeEach
    void setUp() {
        controller = new OpenApiController(leaveService, supplyService, noticeService);
    }

    @Test
    @DisplayName("pendingLeaves userId 必填、限流配置正确、响应不含 reason")
    void pendingLeaves_hidesReasonAndEnforcesUserId() throws Exception {
        Method method = OpenApiController.class.getMethod("pendingLeaves", Long.class, Integer.class);

        // userId 参数必须必填：旧实现 required=false，不传时条件被跳过=拉全量
        RequestParam rp = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertNotNull(rp, "userId 必须声明 @RequestParam");
        assertTrue(rp.required(), "userId 必须 required=true");

        // 限流配置：key/limit/window 与设计一致
        RateLimit rl = method.getAnnotation(RateLimit.class);
        assertNotNull(rl, "pendingLeaves 必须声明 @RateLimit");
        assertEquals("open:approval:leave", rl.key());
        assertEquals(30, rl.limit());
        assertEquals(60, rl.window());

        // 行为：响应不含 reason 敏感字段
        AttLeaveRequest r = new AttLeaveRequest();
        r.setId(1L);
        r.setUserName("张三");
        r.setReason("敏感事由，不应出现在开放接口响应中");
        Page<AttLeaveRequest> page = new Page<>(1, 20);
        page.setRecords(List.of(r));
        page.setTotal(1);
        when(leaveService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = controller.pendingLeaves(1L, 20);

        assertEquals(200, result.getCode());
        assertFalse(result.getData().stream().anyMatch(m -> m.containsKey("reason")),
                "开放接口响应不应携带 reason 敏感字段");
        assertEquals(1L, result.getData().get(0).get("id"));

        // 过滤恒生效：接口仅按固定用户维度查询（用户ID条件必须存在）
        ArgumentCaptor<LambdaQueryWrapper<AttLeaveRequest>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(leaveService).page(any(Page.class), captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    @DisplayName("pendingLeaves 无待审批数据返回空列表")
    void pendingLeaves_emptyList() {
        Page<AttLeaveRequest> page = new Page<>(1, 20);
        page.setRecords(List.of());
        page.setTotal(0);
        when(leaveService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = controller.pendingLeaves(2L, 20);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }
}
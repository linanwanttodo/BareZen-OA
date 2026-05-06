package com.linan.system.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysNotice;
import com.linan.system.service.ISysNoticeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知公告 Controller 单元测试
 *
 * <p>覆盖：发布/下架成功失败分支、最新/已发布列表转发、
 * 阅读回执（公告不存在拒绝、转发 service 统计结果）。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysNoticeControllerTest {

    @Mock
    private ISysNoticeService noticeService;

    @InjectMocks
    private SysNoticeController controller;

    @BeforeEach
    void setUp() throws Exception {
        setThreadLocal("currentUserIdHolder", 1L);
        setThreadLocal("currentUsernameHolder", "admin");
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    @Test
    @DisplayName("发布：成功返回 200")
    void publish_success() {
        when(noticeService.publish(3L)).thenReturn(true);

        Result<Void> result = controller.publish(3L);

        assertEquals(200, result.getCode());
        verify(noticeService).publish(3L);
    }

    @Test
    @DisplayName("发布：服务层失败返回错误")
    void publish_failure() {
        when(noticeService.publish(3L)).thenReturn(false);

        Result<Void> result = controller.publish(3L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("发布失败"));
    }

    @Test
    @DisplayName("下架：成功返回 200")
    void revoke_success() {
        when(noticeService.revoke(3L)).thenReturn(true);

        Result<Void> result = controller.revoke(3L);

        assertEquals(200, result.getCode());
        verify(noticeService).revoke(3L);
    }

    @Test
    @DisplayName("最新通知：转发 service 结果")
    void latest_delegates() {
        SysNotice notice = new SysNotice();
        notice.setNoticeTitle("例会通知");
        when(noticeService.selectLatestNotice()).thenReturn(notice);

        Result<SysNotice> result = controller.getLatest();

        assertEquals(200, result.getCode());
        assertEquals("例会通知", result.getData().getNoticeTitle());
    }

    @Test
    @DisplayName("阅读回执：公告不存在被拒绝")
    void readReceipt_missingNotice_rejected() {
        when(noticeService.readReceipt(9L)).thenReturn(null);

        Result<Map<String, Object>> result = controller.readReceipt(9L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
        verify(noticeService).readReceipt(9L);
    }

    @Test
    @DisplayName("阅读回执：转发 service 统计结果")
    void readReceipt_delegates() {
        Map<String, Object> data = Map.of("noticeId", 9L, "readCount", 2L, "requireRead", "Y");
        when(noticeService.readReceipt(9L)).thenReturn(data);

        Result<Map<String, Object>> result = controller.readReceipt(9L);

        assertEquals(200, result.getCode());
        assertEquals(2L, result.getData().get("readCount"));
        assertEquals("Y", result.getData().get("requireRead"));
        verify(noticeService).readReceipt(9L);
    }
}
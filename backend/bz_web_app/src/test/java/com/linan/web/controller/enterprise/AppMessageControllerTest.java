package com.linan.web.controller.enterprise;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysMessage;
import com.linan.system.service.ISysMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppMessageController 消息中心单测
 *
 * <p>覆盖：分页/未读数/单条已读（本人校验）/全部已读；未登录一律拒绝。</p>
 */
@ExtendWith(MockitoExtension.class)
class AppMessageControllerTest {

    @Mock
    private ISysMessageService messageService;

    private AppMessageController controller;

    @BeforeEach
    void setUp() {
        controller = new AppMessageController(messageService);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    private static void setCurrentUser(Long userId) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField("currentUserIdHolder");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(userId);
    }

    @Test
    @DisplayName("未登录：分页/未读数/已读接口全部拒绝")
    void unauthenticated_rejected() throws Exception {
        Result<?> page = controller.page(1, 10, null);
        assertEquals(500, page.getCode());

        Result<?> count = controller.unreadCount();
        assertEquals(500, count.getCode());

        Result<?> read = controller.markRead(1L);
        assertEquals(500, read.getCode());

        Result<?> all = controller.markAllRead();
        assertEquals(500, all.getCode());

        verify(messageService, never()).pageMine(any(), any(), any());
    }

    @Test
    @DisplayName("分页查询：透传当前用户与已读过滤")
    void page_usesCurrentUser() throws Exception {
        setCurrentUser(10L);
        when(messageService.pageMine(any(), any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysMessage>());

        controller.page(1, 10, 0);

        verify(messageService).pageMine(any(), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(0));
    }

    @Test
    @DisplayName("未读数：返回 service 统计")
    void unreadCount_returnsServiceValue() throws Exception {
        setCurrentUser(10L);
        when(messageService.unreadCount(10L)).thenReturn(3L);

        Result<Long> result = controller.unreadCount();

        assertEquals(200, result.getCode());
        assertEquals(3L, result.getData());
    }

    @Test
    @DisplayName("标记已读：失败返回错误码")
    void markRead_failure_returnsError() throws Exception {
        setCurrentUser(10L);
        when(messageService.markRead(1L, 10L)).thenReturn(false);

        Result<Boolean> result = controller.markRead(1L);

        assertEquals(500, result.getCode());
        verify(messageService, never()).markAllRead(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("全部已读：返回更新条数")
    void markAllRead_returnsCount() throws Exception {
        setCurrentUser(10L);
        when(messageService.markAllRead(10L)).thenReturn(5);

        Result<Integer> result = controller.markAllRead();

        assertEquals(200, result.getCode());
        assertEquals(5, result.getData());
    }
}
package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.web.service.TodoAggregationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * TodoCenterController 统一待办参数钳制与委托单测
 *
 * <p>聚合与数据库分页已下沉到 {@link TodoAggregationService}（web 编排层 service），
 * 本测试聚焦 Controller：划参数钳制（pageNum&lt;1 / pageSize&lt;1 / pageSize&gt;100 拒绝）
 * 与向服务的委托接线。业务分页见 TodoAggregationServiceImplTest。</p>
 */
@ExtendWith(MockitoExtension.class)
class TodoCenterControllerTest {

    @Mock
    private TodoAggregationService todoService;

    private TodoCenterController controller;

    @BeforeAll
    static void initTableInfoCache() {
        // 参数钳制与委托用例不构建 LambdaWrapper，此处无需 TableInfo 缓存
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new TodoCenterController(todoService);
        setThreadLocal("currentUserIdHolder", 1L);
    }

    @AfterEach
    void tearDown() throws Exception {
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
    @DisplayName("pageNum=0 抛出 400 参数异常，不触达聚合服务")
    void list_pageNumZero_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.list(0, 10, null, null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("分页参数不合法"));
        verifyNoInteractions(todoService);
    }

    @Test
    @DisplayName("pageSize=1000 超出业务上限，抛出 400 参数异常")
    void list_pageSizeTooLarge_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.list(1, 1000, null, null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("分页参数不合法"));
        verifyNoInteractions(todoService);
    }

    @Test
    @DisplayName("pageSize=0 抛出 400 参数异常")
    void list_pageSizeZero_rejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.list(1, 0, null, null));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("分页参数不合法"));
        verifyNoInteractions(todoService);
    }

    @Test
    @DisplayName("合法参数：委托聚合服务做数据库分页")
    void list_validParams_delegatesToService() {
        PageResult<TodoAggregationService.TodoItem> pr = new PageResult<>();
        pr.setTotal(2);
        pr.setList(java.util.List.of());
        when(todoService.page(1L, 1, 10, null, null)).thenReturn(pr);

        Result<PageResult<TodoAggregationService.TodoItem>> result = controller.list(1, 10, null, null);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().getTotal());
        verify(todoService).page(1L, 1, 10, null, null);
    }

    @Test
    @DisplayName("summary 委托聚合服务")
    void summary_delegatesToService() {
        when(todoService.summary(1L)).thenReturn(java.util.Map.of("total", 5L));

        Result<java.util.Map<String, Long>> result = controller.summary();

        assertEquals(200, result.getCode());
        assertEquals(5L, result.getData().get("total"));
        verify(todoService).summary(1L);
    }
}
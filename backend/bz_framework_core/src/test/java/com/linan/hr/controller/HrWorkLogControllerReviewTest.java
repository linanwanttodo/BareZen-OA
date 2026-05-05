package com.linan.hr.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.HrWorkLog;
import com.linan.hr.service.IHrWorkLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作日志重复批注拒绝 单测
 *
 * <p>覆盖 T14：同一审批人重复批注拒绝（历史批注只增不改）；他人已批注的日志允许新审批人批注；
 * 未批阅过的日志允许批注；不能批阅自己的日志。</p>
 */
@ExtendWith(MockitoExtension.class)
class HrWorkLogControllerReviewTest {

    @Mock
    private IHrWorkLogService workLogService;

    @InjectMocks
    private HrWorkLogController controller;

    @BeforeEach
    void setUp() throws Exception {
        setCurrentUser(1L);
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
    @DisplayName("同一审批人重复批注被拒绝且不触库")
    void review_sameReviewer_rejected() {
        HrWorkLog log = new HrWorkLog();
        log.setId(1L);
        log.setUserId(2L);          // 日志作者不是当前用户
        log.setReviewerId(1L);      // 已由当前用户批阅过
        when(workLogService.getById(1L)).thenReturn(log);

        Result<Void> result = controller.review(1L, "再批一次");

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("已批阅"));
        assertTrue(result.getMsg().contains("不能重复批注"));
        verify(workLogService, never()).updateById(any());
    }

    @Test
    @DisplayName("未批阅过的日志允许批注")
    void review_notReviewed_allowed() {
        HrWorkLog log = new HrWorkLog();
        log.setId(2L);
        log.setUserId(2L);
        log.setReviewerId(null);    // 从未批阅
        when(workLogService.getById(2L)).thenReturn(log);
        when(workLogService.updateById(any(HrWorkLog.class))).thenReturn(true);

        Result<Void> result = controller.review(2L, "同意");

        assertEquals(200, result.getCode());
        verify(workLogService).updateById(any(HrWorkLog.class));
    }

    @Test
    @DisplayName("他人已批注的日志允许新审批人批注")
    void review_differentReviewer_allowed() {
        HrWorkLog log = new HrWorkLog();
        log.setId(3L);
        log.setUserId(2L);
        log.setReviewerId(99L);     // 已被他人批阅，当前用户未批阅过
        when(workLogService.getById(3L)).thenReturn(log);
        when(workLogService.updateById(any(HrWorkLog.class))).thenReturn(true);

        Result<Void> result = controller.review(3L, "同意");

        assertEquals(200, result.getCode());
        verify(workLogService).updateById(any(HrWorkLog.class));
    }

    @Test
    @DisplayName("不能批阅自己的日志")
    void review_ownLog_rejected() {
        HrWorkLog log = new HrWorkLog();
        log.setId(4L);
        log.setUserId(1L);          // 日志作者 = 当前用户
        when(workLogService.getById(4L)).thenReturn(log);

        Result<Void> result = controller.review(4L, null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不能批阅自己的日志"));
        verify(workLogService, never()).updateById(any());
    }
}
package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysNotice;
import com.linan.system.entity.SysNoticeRead;
import com.linan.system.service.ISysNoticeReadService;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知公告 Controller 单元测试
 *
 * <p>覆盖：发布/下架成功失败分支、最新/已发布列表转发、
 * 阅读回执（公告不存在拒绝、按用户去重统计）。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysNoticeControllerTest {

    @Mock
    private ISysNoticeService noticeService;

    @Mock
    private ISysNoticeReadService noticeReadService;

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
        when(noticeService.getById(9L)).thenReturn(null);

        Result<Map<String, Object>> result = controller.readReceipt(9L);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("不存在"));
        verify(noticeReadService, never()).lambdaQuery();
    }

    @Test
    @DisplayName("阅读回执：同一用户多次阅读去重计数")
    void readReceipt_readCountDistinctByUser() {
        SysNotice notice = new SysNotice();
        notice.setId(9L);
        notice.setRequireRead("Y");
        when(noticeService.getById(9L)).thenReturn(notice);

        // 同一用户(5)阅读两次，另一用户(8)阅读一次 → 去重后 readCount = 2
        SysNoticeRead r1 = read(9L, 5L);
        SysNoticeRead r2 = read(9L, 5L);
        SysNoticeRead r3 = read(9L, 8L);
        stubLambdaQueryReturns(List.of(r1, r2, r3));

        Result<Map<String, Object>> result = controller.readReceipt(9L);

        assertEquals(200, result.getCode());
        assertEquals(2L, result.getData().get("readCount"), "已读用户数应按 userId 去重");
        assertEquals("Y", result.getData().get("requireRead"));
    }

    private SysNoticeRead read(Long noticeId, Long userId) {
        SysNoticeRead r = new SysNoticeRead();
        r.setNoticeId(noticeId);
        r.setUserId(userId);
        r.setReadTime(java.time.LocalDateTime.of(2026, 8, 5, 10, 0));
        return r;
    }

    /** stub lambdaQuery() 链式查询返回给定已读记录 */
    private void stubLambdaQueryReturns(List<SysNoticeRead> reads) {
        @SuppressWarnings("unchecked")
        BaseMapper<SysNoticeRead> mapper = org.mockito.Mockito.mock(BaseMapper.class);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(reads);
        LambdaQueryChainWrapper<SysNoticeRead> chain = new LambdaQueryChainWrapper<SysNoticeRead>(mapper);
        when(noticeReadService.lambdaQuery()).thenReturn(chain);
    }
}
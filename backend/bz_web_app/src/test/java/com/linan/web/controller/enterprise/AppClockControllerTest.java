package com.linan.web.controller.enterprise;

import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttClockRecord;
import com.linan.hr.service.IAttClockRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AppClockController 单元测试
 *
 * <p>覆盖：未登录拦截；打卡身份取 JWT 而非请求体；业务失败（重复打卡/未上班）透传提示；
 * 月份参数解析与透传；今日状态查询。</p>
 *
 * @author BareZen Team
 */
@ExtendWith(MockitoExtension.class)
class AppClockControllerTest {

    @Mock
    private IAttClockRecordService clockService;

    private AppClockController controller;

    @BeforeEach
    void setUp() {
        controller = new AppClockController(clockService);
    }

    @AfterEach
    void tearDown() {
        AuthInterceptor.clearCurrentUser();
    }

    /** 反射注入当前登录用户（AuthInterceptor 的 ThreadLocal 无公开 setter） */
    private static void setCurrentUser(Long userId, String username) throws Exception {
        setThreadLocal("currentUserIdHolder", userId);
        setThreadLocal("currentUsernameHolder", username);
    }

    private static void setThreadLocal(String fieldName, Object value) throws Exception {
        Field field = AuthInterceptor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Object> tl = (ThreadLocal<Object>) field.get(null);
        tl.set(value);
    }

    private AttClockRecord record(LocalDateTime in, LocalDateTime out) {
        AttClockRecord r = new AttClockRecord();
        r.setId(1L);
        r.setUserId(100L);
        r.setUserName("张三");
        r.setClockDate(LocalDate.of(2026, 3, 14));
        r.setClockInTime(in);
        r.setClockOutTime(out);
        r.setStatus("normal");
        return r;
    }

    @Test
    @DisplayName("未登录：四个接口全部返回请先登录且不触服务")
    void notLoggedIn_allRejected() {
        assertEquals(500, controller.clockIn().getCode());
        assertEquals(500, controller.clockOut().getCode());
        assertEquals(500, controller.month("2026-03").getCode());
        assertEquals(500, controller.today().getCode());
        verify(clockService, never()).clockIn(any(), any(), any());
        verify(clockService, never()).clockOut(any(), any(), any());
        verify(clockService, never()).listMonth(any(), any());
        verify(clockService, never()).today(any(), any());
    }

    @Test
    @DisplayName("上班打卡：身份从 JWT 取，userId/userName 服务端填充")
    void clockIn_identityFromJwt() throws Exception {
        setCurrentUser(100L, "张三");
        AttClockRecord record = record(LocalDateTime.of(2026, 3, 14, 9, 0), null);
        when(clockService.clockIn(eq(100L), eq("张三"), any(LocalDateTime.class))).thenReturn(record);

        var result = controller.clockIn();

        assertEquals(200, result.getCode());
        assertEquals("张三", result.getData().getUserName());
        ArgumentCaptor<Long> uid = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        verify(clockService).clockIn(uid.capture(), name.capture(), any(LocalDateTime.class));
        assertEquals(100L, uid.getValue());
        assertEquals("张三", name.getValue());
    }

    @Test
    @DisplayName("下班打卡成功返回更新后记录")
    void clockOut_success() throws Exception {
        setCurrentUser(100L, "张三");
        AttClockRecord record = record(LocalDateTime.of(2026, 3, 14, 9, 0),
                LocalDateTime.of(2026, 3, 14, 18, 30));
        when(clockService.clockOut(eq(100L), eq("张三"), any(LocalDateTime.class))).thenReturn(record);

        var result = controller.clockOut();

        assertEquals(200, result.getCode());
        assertEquals(record, result.getData());
    }

    @Test
    @DisplayName("业务失败（重复打卡/未上班）向上抛业务异常，由全局处理器统一收口")
    void businessFailure_propagatesToGlobalHandler() throws Exception {
        setCurrentUser(100L, "张三");
        when(clockService.clockIn(eq(100L), eq("张三"), any(LocalDateTime.class)))
                .thenThrow(new BusinessException("今日已打卡上班，请勿重复打卡"));

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.clockIn());
        assertTrue(ex.getMessage().contains("重复打卡"));
        // 控制器不再内联 try/catch，异常交由 GlobalExceptionHandler 转换为 Result
        verify(clockService).clockIn(eq(100L), eq("张三"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("月份：缺省用当月，非法格式报错")
    void month_parse() throws Exception {
        setCurrentUser(100L, "张三");

        // 缺省 → 当前月（只验证返回 200 且透传 YearMonth 给服务）
        when(clockService.listMonth(any(), any(YearMonth.class))).thenReturn(List.of());
        assertEquals(200, controller.month(null).getCode());
        verify(clockService).listMonth(100L, YearMonth.now());

        // 非法格式
        var bad = controller.month("2026/03");
        assertEquals(500, bad.getCode());
        assertTrue(bad.getMsg().contains("yyyy-MM"));

        // 合法格式透传
        when(clockService.listMonth(any(), any(YearMonth.class))).thenReturn(List.of());
        assertEquals(200, controller.month("2026-03").getCode());
        verify(clockService).listMonth(100L, YearMonth.of(2026, 3));
    }

    @Test
    @DisplayName("今日状态：无记录返回 null 数据")
    void today_returnsDataOrNull() throws Exception {
        setCurrentUser(100L, "张三");
        when(clockService.today(eq(100L), any(LocalDateTime.class))).thenReturn(null);

        var result = controller.today();

        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }
}
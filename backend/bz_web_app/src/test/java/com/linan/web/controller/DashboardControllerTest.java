package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.monitor.OnlineUserService;
import com.linan.system.service.ISysFileService;
import com.linan.system.service.ISysUserService;
import com.linan.web.service.DashboardStatsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * DashboardController 仪表盘图表数据聚合单元测试
 *
 * <p>仪表盘数据聚合下沉至 {@link DashboardStatsService}，本测试聚焦 Controller
 * 对各服务的委托装配，验证各指标正确聚合到返回 Map 中。</p>
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private OnlineUserService onlineUserService;

    @Mock
    private ISysFileService fileService;

    @Mock
    private DashboardStatsService statsService;

    private DashboardController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new DashboardController(userService, onlineUserService, fileService, statsService);
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
    @DisplayName("charts 聚合所有维度指标并返回成功")
    void charts_aggregatesAllMetrics() {
        List<Map<String, Object>> trend = List.of(Map.of("date", "2026-01-01", "count", 10));
        List<Map<String, Object>> roleDist = List.of(Map.of("name", "Admin", "value", 1));
        List<Map<String, Object>> approvalStats = List.of(Map.of("type", "leave", "pending", 5));

        when(statsService.visitTrend()).thenReturn(trend);
        when(statsService.roleDistribution()).thenReturn(roleDist);
        when(userService.count()).thenReturn(42L);
        when(onlineUserService.countOnlineUsers()).thenReturn(3L);
        when(fileService.count()).thenReturn(100L);
        when(statsService.approvalStats()).thenReturn(approvalStats);

        Result<Map<String, Object>> result = controller.charts();

        assertEquals(200, result.getCode());
        Map<String, Object> data = result.getData();
        assertEquals(trend, data.get("visitTrend"));
        assertEquals(roleDist, data.get("roleDistribution"));
        assertEquals(42L, data.get("userCount"));
        assertEquals(3L, data.get("onlineCount"));
        assertEquals(100L, data.get("fileCount"));
        assertEquals(approvalStats, data.get("approvalStats"));
    }
}
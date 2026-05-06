package com.linan.web.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import com.linan.web.service.impl.DashboardStatsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DashboardStatsServiceImpl 仪表盘聚合单测
 *
 * <p>验证：近 7 天操作日志趋势（逐日计数）、角色分布透传、
 * 审批效率统计（白名单 5 表聚合 SQL、驳回率/平均时长计算）。</p>
 */
@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceImplTest {

    @Mock
    private ISysOperLogService operLogService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DashboardStatsServiceImpl statsService;

    @Test
    @DisplayName("visitTrend：返回近 7 天逐日操作日志计数")
    void visitTrend_last7Days() {
        BaseMapper<SysOperLog> mapper = mock(BaseMapper.class);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L);
        when(operLogService.lambdaQuery()).thenReturn(new LambdaQueryChainWrapper<>(mapper));

        List<Map<String, Object>> trend = statsService.visitTrend();

        // 期望日期序列与实现一致（今天-6 … 今天）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        List<String> expectedDays = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            expectedDays.add(sdf.format(cal.getTime()));
        }

        assertEquals(7, trend.size());
        for (int i = 0; i < 7; i++) {
            assertEquals(expectedDays.get(i), trend.get(i).get("date"));
            assertEquals(i + 1L, trend.get(i).get("count"));
        }
    }

    @Test
    @DisplayName("roleDistribution：SQL 结果原样透传")
    void roleDistribution_passthrough() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", "管理员");
        row.put("value", 2);
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(row));

        List<Map<String, Object>> result = statsService.roleDistribution();

        assertEquals(1, result.size());
        assertEquals("管理员", result.get(0).get("name"));
        assertEquals(2, result.get(0).get("value"));
    }

    @Test
    @DisplayName("approvalStats：白名单 5 表聚合，驳回率按已处理单据计算")
    void approvalStats_computePerTable() {
        // 逐表返回 total/pending/rejected/avgHours
        when(jdbcTemplate.queryForMap(anyString())).thenReturn(
                row(10, 3, 1, 12.5),   // 处理 7 单 → 驳回率 14.3
                row(5, 0, 2, 8.0),     // 处理 5 单 → 驳回率 40.0
                row(0, 0, 0, 0.0),     // 无单据 → 驳回率 0
                row(4, 4, 0, 6.0),     // 处理 0 单 → 驳回率 0
                row(2, 1, 1, 3.5));    // 处理 1 单 → 驳回率 100.0

        List<Map<String, Object>> stats = statsService.approvalStats();

        assertEquals(5, stats.size());

        Map<String, Object> first = stats.get(0);
        assertEquals("att_leave_request", first.get("type"));
        assertEquals("请假审批", first.get("typeName"));
        assertEquals(10L, first.get("total"));
        assertEquals(3L, first.get("pending"));
        assertEquals(1L, first.get("rejected"));
        assertEquals(12.5, first.get("avgHours"));
        assertEquals(14.3, ((Number) first.get("rejectRate")).doubleValue(), 1e-9);

        // 已处理为 0 时驳回率恒为 0
        assertEquals(0.0, ((Number) stats.get(2).get("rejectRate")).doubleValue(), 1e-9);
        assertEquals(0.0, ((Number) stats.get(3).get("rejectRate")).doubleValue(), 1e-9);
        assertEquals(100.0, ((Number) stats.get(4).get("rejectRate")).doubleValue(), 1e-9);
    }

    @Test
    @DisplayName("approvalStats：聚合 SQL 覆盖白名单全部 5 张业务表")
    void approvalStats_queriesAllWhitelistTables() {
        when(jdbcTemplate.queryForMap(anyString())).thenReturn(
                row(1, 0, 0, 0.0), row(1, 0, 0, 0.0), row(1, 0, 0, 0.0),
                row(1, 0, 0, 0.0), row(1, 0, 0, 0.0));

        statsService.approvalStats();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(5)).queryForMap(sqlCaptor.capture());
        List<String> sqls = sqlCaptor.getAllValues();
        assertEquals(5, sqls.size());
        for (String table : List.of("att_leave_request", "att_overtime_request",
                "att_travel_request", "office_vehicle_request", "office_supply_request")) {
            assertTrue(sqls.stream().anyMatch(sql -> sql.contains(table)),
                    "聚合 SQL 应覆盖表 " + table);
        }
    }

    private Map<String, Object> row(int total, int pending, int rejected, double avgHours) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", (long) total);
        m.put("pending", (long) pending);
        m.put("rejected", (long) rejected);
        m.put("avgHours", avgHours);
        return m;
    }
}
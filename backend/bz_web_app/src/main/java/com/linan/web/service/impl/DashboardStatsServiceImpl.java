package com.linan.web.service.impl;

import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import com.linan.web.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘数据聚合服务实现
 *
 * <p>审批效率统计为跨 5 张同构业务表的聚合（status 语义一致、approve_time 文本
 * 存储、del_flag 过滤），用白名单表名映射 + JdbcTemplate 单条聚合 SQL 实现，
 * 表名来自常量映射（非用户输入），无注入面。趋势/分布走 MyBatis-Plus 查询。</p>
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class DashboardStatsServiceImpl implements DashboardStatsService {

    /** 审批流表名 -> 流程类型中文名（白名单，禁止拼接用户输入） */
    private static final Map<String, String> APPROVAL_TABLES = new LinkedHashMap<>();
    static {
        APPROVAL_TABLES.put("att_leave_request", "请假审批");
        APPROVAL_TABLES.put("att_overtime_request", "加班审批");
        APPROVAL_TABLES.put("att_travel_request", "出差审批");
        APPROVAL_TABLES.put("office_vehicle_request", "用车审批");
        APPROVAL_TABLES.put("office_supply_request", "用品申领审批");
    }

    private final ISysOperLogService operLogService;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> visitTrend() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();

        // 近 7 天日期列表
        List<String> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            days.add(sdf.format(cal.getTime()));
        }

        // 逐天统计操作日志数（7 次小查询，量级恒定可接受）
        List<Map<String, Object>> result = new ArrayList<>();
        for (String day : days) {
            Long count = operLogService.lambdaQuery()
                    .apply("DATE(oper_time) = {0}", day)
                    .count();
            Map<String, Object> item = new HashMap<>();
            item.put("date", day);
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> roleDistribution() {
        // 常量 SQL（无用户输入），分组统计用户-角色分布
        String sql = """
            SELECT r.role_name AS name, COUNT(ur.user_id) AS value
            FROM sys_role r
            LEFT JOIN sys_user_role ur ON r.id = ur.role_id
            GROUP BY r.id, r.role_name
            ORDER BY value DESC
        """;
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> approvalStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map.Entry<String, String> entry : APPROVAL_TABLES.entrySet()) {
            String table = entry.getKey();
            String sql = """
                SELECT
                    COUNT(*) AS total,
                    SUM(CASE WHEN status = '0' THEN 1 ELSE 0 END) AS pending,
                    SUM(CASE WHEN status = '2' THEN 1 ELSE 0 END) AS rejected,
                    COALESCE(ROUND(AVG(CASE
                        WHEN status IN ('1','2') AND approve_time IS NOT NULL AND approve_time <> ''
                        THEN TIMESTAMPDIFF(HOUR, create_time, STR_TO_DATE(approve_time, '%%Y-%%m-%%d %%H:%%i:%%s'))
                    END), 1), 0) AS avgHours
                FROM `%s`
                WHERE del_flag = 0
            """.formatted(table);
            Map<String, Object> row = jdbcTemplate.queryForMap(sql);
            long total = ((Number) row.get("total")).longValue();
            long pending = ((Number) row.get("pending")).longValue();
            long rejected = ((Number) row.get("rejected")).longValue();
            long processed = total - pending; // 已处理（通过+驳回）
            double rejectRate = processed > 0 ? Math.round(rejected * 1000.0 / processed) / 10.0 : 0.0;

            Map<String, Object> item = new HashMap<>();
            item.put("type", entry.getKey());
            item.put("typeName", entry.getValue());
            item.put("total", total);
            item.put("pending", pending);
            item.put("rejected", rejected);
            item.put("avgHours", row.get("avgHours"));
            item.put("rejectRate", rejectRate);
            stats.add(item);
        }
        return stats;
    }
}
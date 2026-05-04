package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import com.linan.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 管理端仪表盘数据聚合 Controller
 */
@Tag(name = "仪表盘", description = "管理端仪表盘数据聚合")
@RestController
@RequestMapping("/api/system/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ISysOperLogService operLogService;

    private final ISysUserService userService;

    private final JdbcTemplate jdbcTemplate;

    private final com.linan.system.monitor.OnlineUserService onlineUserService;

    private final com.linan.system.service.ISysFileService fileService;

    /**
     * 获取图表数据
     * GET /api/system/v1/dashboard/charts
     */
    @RequirePermission("system:dashboard:list")
    @Operation(summary = "获取仪表盘图表数据")
    @GetMapping("/charts")
    public Result<Map<String, Object>> charts() {
        Map<String, Object> data = new HashMap<>();

        // 1. 近7天操作日志趋势
        data.put("visitTrend", getVisitTrend());

        // 2. 用户角色分布
        data.put("roleDistribution", getRoleDistribution());

        // 3. 用户总数
        long userCount = userService.count();
        data.put("userCount", userCount);

        // 4. 在线用户数
        long onlineCount = onlineUserService.countOnlineUsers();
        data.put("onlineCount", onlineCount);

        // 5. 文件总数
        long fileCount = fileService.count();
        data.put("fileCount", fileCount);

        // 6. 审批效率统计（各审批流待办/平均时长/驳回率）
        data.put("approvalStats", getApprovalStats());

        return Result.success(data);
    }

    /**
     * 审批效率统计：聚合各审批表（请假/加班/出差/用车/用品申领）的
     * 待审批数、平均审批时长（小时）、驳回率，支撑管理层数据驱动决策。
     *
     * <p>各审批表结构一致（status 0=待审批 1=通过 2=驳回；create_time 创建、
     * approve_time 审批），统一按表名聚合。approve_time 为 yyyy-MM-dd HH:mm:ss 文本，
     * 用 TIMESTAMPDIFF 计算耗时；驳回率 = 驳回数 / 已处理数。</p>
     */
    private List<Map<String, Object>> getApprovalStats() {
        // 表名 -> 流程类型中文名
        Map<String, String> tables = new LinkedHashMap<>();
        tables.put("att_leave_request", "请假审批");
        tables.put("att_overtime_request", "加班审批");
        tables.put("att_travel_request", "出差审批");
        tables.put("office_vehicle_request", "用车审批");
        tables.put("office_supply_request", "用品申领审批");

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Map.Entry<String, String> entry : tables.entrySet()) {
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

    private List<Map<String, Object>> getVisitTrend() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();

        // 生成近7天日期列表
        List<String> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            days.add(sdf.format(cal.getTime()));
        }

        // 从DB统计每天的操作日志数
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRoleDistribution() {
        String sql = """
            SELECT r.role_name AS name, COUNT(ur.user_id) AS value
            FROM sys_role r
            LEFT JOIN sys_user_role ur ON r.id = ur.role_id
            GROUP BY r.id, r.role_name
            ORDER BY value DESC
        """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return rows;
    }
}

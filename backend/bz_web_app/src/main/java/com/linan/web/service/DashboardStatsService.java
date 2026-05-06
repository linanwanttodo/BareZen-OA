package com.linan.web.service;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘数据聚合服务
 *
 * <p>管理端图表数据的只读聚合（近 7 天操作日志趋势 / 角色分布 / 审批效率统计），
 * 归属 web 编排层 service，避免 Controller 直接持有 JdbcTemplate 裸 SQL。</p>
 *
 * @author BareZen Team
 */
public interface DashboardStatsService {

    /** 近 7 天操作日志趋势 [{date, count}] */
    List<Map<String, Object>> visitTrend();

    /** 用户角色分布 [{name, value}] */
    List<Map<String, Object>> roleDistribution();

    /** 审批效率统计（各审批流 待办/平均时长/驳回率） */
    List<Map<String, Object>> approvalStats();
}
package com.linan.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.article.entity.ArtArticle;
import com.linan.article.service.IArtArticleService;
import com.linan.core.common.api.Result;
import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import com.linan.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端仪表盘数据聚合 Controller
 */
@RestController
@RequestMapping("/api/system/v1/dashboard")
public class DashboardController {

    @Autowired
    private ISysOperLogService operLogService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IArtArticleService articleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取图表数据
     * GET /api/system/v1/dashboard/charts
     */
    @GetMapping("/charts")
    public Result<Map<String, Object>> charts() {
        Map<String, Object> data = new HashMap<>();

        // 1. 近7天操作日志趋势 — 每天的操作次数作为"访问量"
        data.put("visitTrend", getVisitTrend());

        // 2. 用户角色分布 — 统计每个角色的用户数
        data.put("roleDistribution", getRoleDistribution());

        // 3. 文章总数
        long articleCount = articleService.count();
        data.put("articleCount", articleCount);

        // 4. 近7天新增文章趋势
        data.put("articleTrend", getArticleTrend());

        return Result.success(data);
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

    private List<Map<String, Object>> getArticleTrend() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();

        List<String> days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            days.add(sdf.format(cal.getTime()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String day : days) {
            Long count = articleService.lambdaQuery()
                    .apply("DATE(create_time) = {0}", day)
                    .count();
            Map<String, Object> item = new HashMap<>();
            item.put("date", day);
            item.put("count", count);
            result.add(item);
        }
        return result;
    }
}

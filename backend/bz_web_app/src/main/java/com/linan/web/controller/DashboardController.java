package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.monitor.OnlineUserService;
import com.linan.system.service.ISysFileService;
import com.linan.system.service.ISysUserService;
import com.linan.web.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 管理端仪表盘数据聚合 Controller
 *
 * <p>图表聚合（趋势/角色分布/审批效率）下沉至 {@link DashboardStatsService}，
 * 本类仅做指标装配与委托。</p>
 */
@Tag(name = "仪表盘", description = "管理端仪表盘数据聚合")
@RestController
@RequestMapping("/api/system/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ISysUserService userService;

    private final OnlineUserService onlineUserService;

    private final ISysFileService fileService;

    private final DashboardStatsService statsService;

    /**
     * 获取图表数据
     * GET /api/system/v1/dashboard/charts
     */
    @RequirePermission("system:dashboard:list")
    @Operation(summary = "获取仪表盘图表数据")
    @GetMapping("/charts")
    public Result<Map<String, Object>> charts() {
        Map<String, Object> data = new HashMap<>();
        data.put("visitTrend", statsService.visitTrend());
        data.put("roleDistribution", statsService.roleDistribution());
        data.put("userCount", userService.count());
        data.put("onlineCount", onlineUserService.countOnlineUsers());
        data.put("fileCount", fileService.count());
        data.put("approvalStats", statsService.approvalStats());
        return Result.success(data);
    }
}
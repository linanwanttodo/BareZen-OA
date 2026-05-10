package com.linan.web.controller.open;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.RateLimit;
import com.linan.core.common.api.Result;
import com.linan.core.security.OpenScope;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.office.entity.OfficeSupply;
import com.linan.office.service.IOfficeSupplyService;
import com.linan.system.entity.SysNotice;
import com.linan.system.service.ISysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 开放平台示例接口（/api/open/**）
 *
 * <p>供外部系统（如钉钉/企微集成、其他业务系统）通过 API Key 调用，
 * 对应市场调研中「系统集成」需求。外部系统在管理端生成 API Key 后，
 * 请求头携带 {@code X-Api-Key} 即可调用；方法级 {@code @OpenScope}
 * 声明所需权限范围（如 open:approval:read）。</p>
 *
 * <p>鉴权链路：OpenApiInterceptor（X-Api-Key 校验）→ OpenScopeAspect（范围校验）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "开放平台", description = "Open API - 外部系统集成接口")
@RestController
@RequestMapping("/api/open/v1")
@RequiredArgsConstructor
public class OpenApiController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IAttLeaveRequestService leaveService;

    private final IOfficeSupplyService supplyService;

    private final ISysNoticeService noticeService;

    /**
     * 健康检查（外部系统连通性探测）
     */
    @OpenScope("open:health")
    @Operation(summary = "开放接口健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "BareZen-OpenAPI");
        data.put("time", LocalDateTime.now().format(FMT));
        return Result.success(data);
    }

    /**
     * 查询指定用户待审批的请假单（供外部系统聚合待办）
     *
     * <p>userId 必填：仅允许按用户维度拉取本人待办，杜绝不传参数拉全量数据；
     * 接口限流防外部系统异常调用打爆数据库；响应不携带请假事由等敏感字段。</p>
     */
    @OpenScope("open:approval:read")
    @RateLimit(key = "open:approval:leave", limit = 30, window = 60)
    @Operation(summary = "查询用户待审批请假单")
    @GetMapping("/approvals/leave/pending")
    public Result<List<Map<String, Object>>> pendingLeaves(
            @Parameter(description = "用户ID", required = true)
            @RequestParam(required = true) Long userId,
            @Parameter(description = "返回条数上限，默认 20，最大 100")
            @RequestParam(defaultValue = "20") Integer limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Page<AttLeaveRequest> page = leaveService.page(
                new Page<>(1, safeLimit),
                new LambdaQueryWrapper<AttLeaveRequest>()
                        .eq(AttLeaveRequest::getStatus, "0")
                        .eq(AttLeaveRequest::getUserId, userId)
                        .orderByAsc(AttLeaveRequest::getCreateTime));

        List<Map<String, Object>> result = page.getRecords().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("userName", r.getUserName());
            m.put("leaveType", r.getLeaveType());
            m.put("startTime", r.getStartTime());
            m.put("endTime", r.getEndTime());
            m.put("leaveDays", r.getLeaveDays());
            m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().format(FMT) : null);
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 查询启用状态的办公用品列表（供外部系统/看板调用）
     */
    @OpenScope("open:supply:read")
    @Operation(summary = "查询办公用品库存")
    @GetMapping("/supplies")
    public Result<List<Map<String, Object>>> supplies(@RequestParam(defaultValue = "100") Integer limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        Page<OfficeSupply> page = supplyService.page(
                new Page<>(1, safeLimit),
                new LambdaQueryWrapper<OfficeSupply>()
                        .eq(OfficeSupply::getStatus, "0")
                        .orderByDesc(OfficeSupply::getCreateTime));

        List<Map<String, Object>> result = page.getRecords().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("supplyName", s.getSupplyName());
            m.put("category", s.getCategory());
            m.put("stock", s.getStock());
            m.put("unit", s.getUnit());
            m.put("spec", s.getSpec());
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 查询已发布公告列表（供外部系统同步）
     */
    @OpenScope("open:notice:read")
    @Operation(summary = "查询已发布公告")
    @GetMapping("/notices")
    public Result<List<Map<String, Object>>> notices(@RequestParam(defaultValue = "20") Integer limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Page<SysNotice> page = noticeService.page(
                new Page<>(1, safeLimit),
                new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getStatus, "1")
                        .orderByDesc(SysNotice::getCreateTime));

        List<Map<String, Object>> result = page.getRecords().stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("noticeTitle", n.getNoticeTitle());
            m.put("noticeType", n.getNoticeType());
            m.put("createTime", n.getCreateTime() != null ? n.getCreateTime().format(FMT) : null);
            return m;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}

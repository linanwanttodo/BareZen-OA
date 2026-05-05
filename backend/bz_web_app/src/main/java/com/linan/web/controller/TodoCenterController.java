package com.linan.web.controller;

import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.entity.OfficeVehicleRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.office.service.IOfficeVehicleRequestService;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.service.IApprovalFlowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一待办中心 Controller
 *
 * <p>聚合各业务模块中"待我审批"的事项。V36 多级审批引擎接入后，
 * 请假/加班/出差/用车 4 类以 approval_flow_task 当前待审节点为准
 * （指定审批人节点仅本人可见），用品申领保持按业务表 status='0' 聚合。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "统一待办中心", description = "聚合各模块待审批事项")
@RestController
@RequestMapping("/api/system/v1/todo-center")
@RequiredArgsConstructor
public class TodoCenterController {

    private final IAttLeaveRequestService leaveService;

    private final IAttOvertimeRequestService overtimeService;

    private final IAttTravelRequestService travelService;

    private final IOfficeVehicleRequestService vehicleService;

    private final IOfficeSupplyRequestService supplyRequestService;

    /** 审批节点任务（V36 引擎） */
    private final IApprovalFlowTaskService taskService;

    /**
     * 待办事项类型
     */
    public record TodoItem(
            String type,      // leave / overtime / travel / vehicle / supply
            String typeName,  // 类型中文名（前端也可按 type 自行映射）
            String id,        // 业务记录 ID
            String title,     // 待办标题
            String applicantName,
            String startTime,
            String endTime,
            String createTime) {
    }

    /**
     * 分页聚合查询待办事项（引擎待审节点 4 类 + 用品申领 1 类）
     */
    @RequirePermission("system:todo:list")
    @Operation(summary = "分页查询统一待办")
    @GetMapping("/page")
    public Result<PageResult<TodoItem>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 参数钳制：pageNum/负值或 pageSize 越界直接拒绝，避免 subList 负索引与无界内存分页
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            return Result.error("分页参数不合法");
        }

        Long userId = AuthInterceptor.getCurrentUserId();

        // 1. 引擎当前待审节点（4 类；指定审批人节点仅本人可见，any 节点对所有审批人可见）
        List<ApprovalFlowTask> pendingTasks = taskService.lambdaQuery()
                .eq(ApprovalFlowTask::getStatus, "0")
                .and(w -> w.isNull(ApprovalFlowTask::getApproverId)
                        .or().eq(ApprovalFlowTask::getApproverId, userId))
                .orderByDesc(ApprovalFlowTask::getCreateTime)
                .last("LIMIT 1000")
                .list();

        // 2. 按 bizType 收集业务ID，批量查业务表（避免 N+1）
        Map<String, Set<Long>> idsByType = pendingTasks.stream()
                .collect(Collectors.groupingBy(ApprovalFlowTask::getBizType,
                        Collectors.mapping(ApprovalFlowTask::getBizId, Collectors.toSet())));

        Map<Long, AttLeaveRequest> leaveMap = queryByIds(idsByType.get("leave"),
                ids -> leaveService.lambdaQuery().in(AttLeaveRequest::getId, ids).list(),
                AttLeaveRequest::getId);
        Map<Long, AttOvertimeRequest> overtimeMap = queryByIds(idsByType.get("overtime"),
                ids -> overtimeService.lambdaQuery().in(AttOvertimeRequest::getId, ids).list(),
                AttOvertimeRequest::getId);
        Map<Long, AttTravelRequest> travelMap = queryByIds(idsByType.get("travel"),
                ids -> travelService.lambdaQuery().in(AttTravelRequest::getId, ids).list(),
                AttTravelRequest::getId);
        Map<Long, OfficeVehicleRequest> vehicleMap = queryByIds(idsByType.get("vehicle"),
                ids -> vehicleService.lambdaQuery().in(OfficeVehicleRequest::getId, ids).list(),
                OfficeVehicleRequest::getId);

        // 3. 按节点顺序组装（引擎单据）
        List<TodoItem> all = new ArrayList<>();
        for (ApprovalFlowTask t : pendingTasks) {
            switch (t.getBizType()) {
                case "leave" -> {
                    AttLeaveRequest e = leaveMap.get(t.getBizId());
                    if (e != null) {
                        all.add(new TodoItem("leave", "请假申请", String.valueOf(e.getId()),
                                e.getUserName() + " 的请假申请", e.getUserName(),
                                e.getStartTime(), e.getEndTime(),
                                fmt(e.getCreateTime())));
                    }
                }
                case "overtime" -> {
                    AttOvertimeRequest e = overtimeMap.get(t.getBizId());
                    if (e != null) {
                        all.add(new TodoItem("overtime", "加班申请", String.valueOf(e.getId()),
                                e.getUserName() + " 的加班申请", e.getUserName(),
                                e.getStartTime(), e.getEndTime(),
                                fmt(e.getCreateTime())));
                    }
                }
                case "travel" -> {
                    AttTravelRequest e = travelMap.get(t.getBizId());
                    if (e != null) {
                        all.add(new TodoItem("travel", "出差申请", String.valueOf(e.getId()),
                                e.getUserName() + " 的出差申请", e.getUserName(),
                                e.getStartTime(), e.getEndTime(),
                                fmt(e.getCreateTime())));
                    }
                }
                case "vehicle" -> {
                    OfficeVehicleRequest e = vehicleMap.get(t.getBizId());
                    if (e != null) {
                        all.add(new TodoItem("vehicle", "用车申请", String.valueOf(e.getId()),
                                e.getApplicantName() + " 的用车申请", e.getApplicantName(),
                                e.getStartTime(), e.getEndTime(),
                                fmt(e.getCreateTime())));
                    }
                }
                default -> {
                    // 未接入引擎的业务类型忽略（不在待办聚合范围）
                }
            }
        }

        // 4. 用品申领：保持按业务表 status='0' 聚合（未接入引擎）
        List<OfficeSupplyRequest> supplies = supplyRequestService.lambdaQuery()
                .eq(OfficeSupplyRequest::getStatus, "0")
                .orderByDesc(OfficeSupplyRequest::getCreateTime)
                .last("LIMIT 1000")
                .list();
        for (OfficeSupplyRequest s : supplies) {
            all.add(new TodoItem("supply", "用品申领", String.valueOf(s.getId()),
                    s.getUserName() + " 申领 " + s.getSupplyName()
                            + (s.getQuantity() != null ? " ×" + s.getQuantity() : ""),
                    s.getUserName(),
                    null, null,
                    fmt(s.getCreateTime())));
        }

        all.sort((a, b) -> {
            String ca = a.createTime(), cb = b.createTime();
            if (ca == null || cb == null) return 0;
            return cb.compareTo(ca);
        });

        // 5. 内存分页
        int total = all.size();
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<TodoItem> pageList = total == 0 ? List.of() : all.subList(from, to);

        PageResult<TodoItem> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);
        pageResult.setPages((int) Math.ceil(total / (double) pageSize));
        pageResult.setList(pageList);

        return Result.success(pageResult);
    }

    /**
     * 待办分类统计（引擎待审节点 4 类 + 用品申领 1 类）
     */
    @RequirePermission("system:todo:list")
    @Operation(summary = "待办分类统计")
    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        Long userId = AuthInterceptor.getCurrentUserId();
        long leaveCount = pendingCount("leave", userId);
        long overtimeCount = pendingCount("overtime", userId);
        long travelCount = pendingCount("travel", userId);
        long vehicleCount = pendingCount("vehicle", userId);
        long supplyCount = supplyRequestService.lambdaQuery()
                .eq(OfficeSupplyRequest::getStatus, "0").count();
        long total = leaveCount + overtimeCount + travelCount + vehicleCount + supplyCount;
        return Result.success(Map.of(
                "leave", leaveCount,
                "overtime", overtimeCount,
                "travel", travelCount,
                "vehicle", vehicleCount,
                "supply", supplyCount,
                "total", total));
    }

    /** 引擎某业务类型当前待审节点数（指定审批人节点仅本人可见） */
    private long pendingCount(String bizType, Long userId) {
        return taskService.lambdaQuery()
                .eq(ApprovalFlowTask::getBizType, bizType)
                .eq(ApprovalFlowTask::getStatus, "0")
                .and(w -> w.isNull(ApprovalFlowTask::getApproverId)
                        .or().eq(ApprovalFlowTask::getApproverId, userId))
                .count();
    }

    /** 批量 in 查询并转 id 索引 Map；空集合直接返回空 Map */
    private <T> Map<Long, T> queryByIds(Set<Long> ids,
                                        Function<List<Long>, List<T>> query,
                                        Function<T, Long> keyExtractor) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return query.apply(List.copyOf(ids)).stream()
                .collect(Collectors.toMap(keyExtractor, Function.identity()));
    }

    private String fmt(java.time.LocalDateTime t) {
        return t != null ? t.toString() : null;
    }
}
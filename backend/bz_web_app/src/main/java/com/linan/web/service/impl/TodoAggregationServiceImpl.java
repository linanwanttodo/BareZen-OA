package com.linan.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
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
import com.linan.web.service.TodoAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一待办聚合服务实现
 *
 * <p>以 approval_flow_task 待审节点为单一数据源做数据库分页（引擎节点，
 * 非内存分页），再对当前页任务按 bizType 批量查业务表补齐标题/时间明细（避免 N+1）。</p>
 *
 * @author BareZen Team
 */
@Service
@RequiredArgsConstructor
public class TodoAggregationServiceImpl implements TodoAggregationService {

    private static final String STATUS_PENDING = "0";

    private final IAttLeaveRequestService leaveService;
    private final IAttOvertimeRequestService overtimeService;
    private final IAttTravelRequestService travelService;
    private final IOfficeVehicleRequestService vehicleService;
    private final IOfficeSupplyRequestService supplyRequestService;
    private final IApprovalFlowTaskService taskService;

    @Override
    public PageResult<TodoItem> page(Long userId, int pageNum, int pageSize) {
        // 引擎待审节点数据库分页：status=0 且（any 节点对所有审批人 或 指定审批人仅本人）
        IPage<ApprovalFlowTask> taskPage = taskService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ApprovalFlowTask>()
                        .eq(ApprovalFlowTask::getStatus, STATUS_PENDING)
                        .and(w -> w.isNull(ApprovalFlowTask::getApproverId)
                                .or().eq(ApprovalFlowTask::getApproverId, userId))
                        .orderByDesc(ApprovalFlowTask::getCreateTime));

        // 按业务类型收集本页 bizId，批量查业务表（避免 N+1）
        Map<String, Set<Long>> idsByType = taskPage.getRecords().stream()
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
        Map<Long, OfficeSupplyRequest> supplyMap = queryByIds(idsByType.get("supply"),
                ids -> supplyRequestService.lambdaQuery().in(OfficeSupplyRequest::getId, ids).list(),
                OfficeSupplyRequest::getId);

        // 按本页节点顺序组装（引擎单据 5 类同构：title=申请人 + 类型）
        List<TodoItem> items = new ArrayList<>();
        for (ApprovalFlowTask t : taskPage.getRecords()) {
            switch (t.getBizType()) {
                case "leave" -> assemble(leaveMap.get(t.getBizId()), "leave", "请假申请",
                        e -> e.getUserName(), e -> e.getStartTime(), e -> e.getEndTime(),
                        e -> fmt(e.getCreateTime()), items);
                case "overtime" -> assemble(overtimeMap.get(t.getBizId()), "overtime", "加班申请",
                        e -> e.getUserName(), e -> e.getStartTime(), e -> e.getEndTime(),
                        e -> fmt(e.getCreateTime()), items);
                case "travel" -> assemble(travelMap.get(t.getBizId()), "travel", "出差申请",
                        e -> e.getUserName(), e -> e.getStartTime(), e -> e.getEndTime(),
                        e -> fmt(e.getCreateTime()), items);
                case "vehicle" -> assemble(vehicleMap.get(t.getBizId()), "vehicle", "用车申请",
                        OfficeVehicleRequest::getApplicantName, OfficeVehicleRequest::getStartTime,
                        OfficeVehicleRequest::getEndTime, e -> fmt(e.getCreateTime()), items);
                case "supply" -> {
                    OfficeSupplyRequest s = supplyMap.get(t.getBizId());
                    if (s != null) {
                        String title = s.getUserName() + " 申领 " + s.getSupplyName()
                                + (s.getQuantity() != null ? " ×" + s.getQuantity() : "");
                        items.add(new TodoItem("supply", "用品申领", String.valueOf(s.getId()), title,
                                s.getUserName(), null, null, fmt(s.getCreateTime())));
                    }
                }
                default -> {
                    // 未接入引擎的业务类型忽略（不在待办聚合范围）
                }
            }
        }

        // 数据库分页元数据 + 当前页组装结果
        PageResult<TodoItem> result = new PageResult<>();
        result.setTotal(taskPage.getTotal());
        result.setPageNum(taskPage.getCurrent());
        result.setPageSize(taskPage.getSize());
        result.setPages(taskPage.getPages());
        result.setList(items);
        return result;
    }

    @Override
    public Map<String, Long> summary(Long userId) {
        long leaveCount = pendingCount("leave", userId);
        long overtimeCount = pendingCount("overtime", userId);
        long travelCount = pendingCount("travel", userId);
        long vehicleCount = pendingCount("vehicle", userId);
        long supplyCount = pendingCount("supply", userId);
        long total = leaveCount + overtimeCount + travelCount + vehicleCount + supplyCount;
        return Map.of(
                "leave", leaveCount,
                "overtime", overtimeCount,
                "travel", travelCount,
                "vehicle", vehicleCount,
                "supply", supplyCount,
                "total", total);
    }

    /** 引擎某业务类型当前待审节点数（指定审批人节点仅本人可见） */
    private long pendingCount(String bizType, Long userId) {
        return taskService.lambdaQuery()
                .eq(ApprovalFlowTask::getBizType, bizType)
                .eq(ApprovalFlowTask::getStatus, STATUS_PENDING)
                .and(w -> w.isNull(ApprovalFlowTask::getApproverId)
                        .or().eq(ApprovalFlowTask::getApproverId, userId))
                .count();
    }

    /** 请假/加班/出差/用车同构组装（业务实体均继承 BaseEntity，主键 id 由基类统一提供） */
    private <T extends com.linan.core.common.entity.BaseEntity> void assemble(T biz, String type, String typeName,
                              Function<T, String> applicant,
                              Function<T, String> startTime,
                              Function<T, String> endTime,
                              Function<T, String> createTime,
                              List<TodoItem> items) {
        if (biz == null) {
            return;
        }
        String applicantName = applicant.apply(biz);
        items.add(new TodoItem(type, typeName, String.valueOf(biz.getId()),
                applicantName + " 的" + typeName, applicantName,
                startTime.apply(biz), endTime.apply(biz), createTime.apply(biz)));
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
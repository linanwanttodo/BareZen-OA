package com.linan.web.controller;

import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
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
import java.util.stream.Collectors;

/**
 * 统一待办中心 Controller
 *
 * <p>聚合各业务模块中"待我审批"的事项（请假、加班、出差、用车、用品申领 5 类），
 * 供管理后台统一展示与处理，避免逐个模块查看。</p>
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
     * 分页聚合查询待办事项（status=0 待审批，5 类）
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

        // 1. 待审批的请假申请（LIMIT 1000 防无界全表扫描）
        List<AttLeaveRequest> leaves = leaveService.lambdaQuery()
                .eq(AttLeaveRequest::getStatus, "0")
                .orderByDesc(AttLeaveRequest::getCreateTime)
                .last("LIMIT 1000")
                .list();

        // 2. 待审批的加班申请（LIMIT 1000 防无界全表扫描）
        List<AttOvertimeRequest> overtimes = overtimeService.lambdaQuery()
                .eq(AttOvertimeRequest::getStatus, "0")
                .orderByDesc(AttOvertimeRequest::getCreateTime)
                .last("LIMIT 1000")
                .list();

        // 3. 待审批的出差申请（LIMIT 1000 防无界全表扫描）
        List<AttTravelRequest> travels = travelService.lambdaQuery()
                .eq(AttTravelRequest::getStatus, "0")
                .orderByDesc(AttTravelRequest::getCreateTime)
                .last("LIMIT 1000")
                .list();

        // 4. 待审批的用车申请（LIMIT 1000 防无界全表扫描）
        List<OfficeVehicleRequest> vehicles = vehicleService.lambdaQuery()
                .eq(OfficeVehicleRequest::getStatus, "0")
                .orderByDesc(OfficeVehicleRequest::getCreateTime)
                .last("LIMIT 1000")
                .list();

        // 5. 待审批的用品申领（LIMIT 1000 防无界全表扫描）
        List<OfficeSupplyRequest> supplies = supplyRequestService.lambdaQuery()
                .eq(OfficeSupplyRequest::getStatus, "0")
                .orderByDesc(OfficeSupplyRequest::getCreateTime)
                .last("LIMIT 1000")
                .list();

        // 6. 组装统一结构并按创建时间倒序
        List<TodoItem> all = new ArrayList<>();
        for (AttLeaveRequest l : leaves) {
            all.add(new TodoItem("leave", "请假申请", String.valueOf(l.getId()),
                    l.getUserName() + " 的请假申请", l.getUserName(),
                    l.getStartTime(), l.getEndTime(),
                    l.getCreateTime() != null ? l.getCreateTime().toString() : null));
        }
        for (AttOvertimeRequest o : overtimes) {
            all.add(new TodoItem("overtime", "加班申请", String.valueOf(o.getId()),
                    o.getUserName() + " 的加班申请", o.getUserName(),
                    o.getStartTime(), o.getEndTime(),
                    o.getCreateTime() != null ? o.getCreateTime().toString() : null));
        }
        for (AttTravelRequest t : travels) {
            all.add(new TodoItem("travel", "出差申请", String.valueOf(t.getId()),
                    t.getUserName() + " 的出差申请", t.getUserName(),
                    t.getStartTime(), t.getEndTime(),
                    t.getCreateTime() != null ? t.getCreateTime().toString() : null));
        }
        for (OfficeVehicleRequest v : vehicles) {
            all.add(new TodoItem("vehicle", "用车申请", String.valueOf(v.getId()),
                    v.getApplicantName() + " 的用车申请", v.getApplicantName(),
                    v.getStartTime(), v.getEndTime(),
                    v.getCreateTime() != null ? v.getCreateTime().toString() : null));
        }
        for (OfficeSupplyRequest s : supplies) {
            all.add(new TodoItem("supply", "用品申领", String.valueOf(s.getId()),
                    s.getUserName() + " 申领 " + s.getSupplyName()
                            + (s.getQuantity() != null ? " ×" + s.getQuantity() : ""),
                    s.getUserName(),
                    null, null,
                    s.getCreateTime() != null ? s.getCreateTime().toString() : null));
        }

        all.sort((a, b) -> {
            String ca = a.createTime(), cb = b.createTime();
            if (ca == null || cb == null) return 0;
            return cb.compareTo(ca);
        });

        // 7. 内存分页
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
     * 待办分类统计（供待办中心角标/仪表盘展示，5 类）
     */
    @RequirePermission("system:todo:list")
    @Operation(summary = "待办分类统计")
    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        long leaveCount = leaveService.lambdaQuery()
                .eq(AttLeaveRequest::getStatus, "0").count();
        long overtimeCount = overtimeService.lambdaQuery()
                .eq(AttOvertimeRequest::getStatus, "0").count();
        long travelCount = travelService.lambdaQuery()
                .eq(AttTravelRequest::getStatus, "0").count();
        long vehicleCount = vehicleService.lambdaQuery()
                .eq(OfficeVehicleRequest::getStatus, "0").count();
        long supplyCount = supplyRequestService.lambdaQuery()
                .eq(OfficeSupplyRequest::getStatus, "0").count();
        long total = leaveCount + overtimeCount + travelCount + vehicleCount + supplyCount;
        return Result.success(java.util.Map.of(
                "leave", leaveCount,
                "overtime", overtimeCount,
                "travel", travelCount,
                "vehicle", vehicleCount,
                "supply", supplyCount,
                "total", total));
    }
}

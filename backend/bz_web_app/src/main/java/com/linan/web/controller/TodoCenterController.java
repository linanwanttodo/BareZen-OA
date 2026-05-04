package com.linan.web.controller;

import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.office.entity.OfficeVehicleRequest;
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
import java.util.stream.Collectors;

/**
 * 统一待办中心 Controller
 *
 * <p>聚合各业务模块中"待我审批"的事项（请假申请、用车申请等），
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

    private final IOfficeVehicleRequestService vehicleService;

    /**
     * 待办事项类型
     */
    public record TodoItem(
            String type,      // leave / vehicle
            String typeName,  // 类型中文名（前端也可按 type 自行映射）
            String id,        // 业务记录 ID
            String title,     // 待办标题
            String applicantName,
            String startTime,
            String endTime,
            String createTime) {
    }

    /**
     * 分页聚合查询待办事项（status=0 待审批）
     */
    @RequirePermission("system:todo:list")
    @Operation(summary = "分页查询统一待办")
    @GetMapping("/page")
    public Result<PageResult<TodoItem>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 1. 待审批的请假申请
        List<AttLeaveRequest> leaves = leaveService.lambdaQuery()
                .eq(AttLeaveRequest::getStatus, "0")
                .orderByDesc(AttLeaveRequest::getCreateTime)
                .list();

        // 2. 待审批的用车申请
        List<OfficeVehicleRequest> vehicles = vehicleService.lambdaQuery()
                .eq(OfficeVehicleRequest::getStatus, "0")
                .orderByDesc(OfficeVehicleRequest::getCreateTime)
                .list();

        // 3. 组装统一结构并按创建时间倒序
        List<TodoItem> all = new ArrayList<>();
        for (AttLeaveRequest l : leaves) {
            all.add(new TodoItem("leave", "请假申请", String.valueOf(l.getId()),
                    l.getUserName() + " 的请假申请", l.getUserName(),
                    l.getStartTime(), l.getEndTime(),
                    l.getCreateTime() != null ? l.getCreateTime().toString() : null));
        }
        for (OfficeVehicleRequest v : vehicles) {
            all.add(new TodoItem("vehicle", "用车申请", String.valueOf(v.getId()),
                    v.getApplicantName() + " 的用车申请", v.getApplicantName(),
                    v.getStartTime(), v.getEndTime(),
                    v.getCreateTime() != null ? v.getCreateTime().toString() : null));
        }

        all.sort((a, b) -> {
            String ca = a.createTime(), cb = b.createTime();
            if (ca == null || cb == null) return 0;
            return cb.compareTo(ca);
        });

        // 4. 内存分页
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
     * 待办分类统计（供待办中心角标/仪表盘展示）
     */
    @RequirePermission("system:todo:list")
    @Operation(summary = "待办分类统计")
    @GetMapping("/summary")
    public Result<java.util.Map<String, Long>> summary() {
        long leaveCount = leaveService.lambdaQuery()
                .eq(AttLeaveRequest::getStatus, "0").count();
        long vehicleCount = vehicleService.lambdaQuery()
                .eq(OfficeVehicleRequest::getStatus, "0").count();
        return Result.success(java.util.Map.of(
                "leave", leaveCount,
                "vehicle", vehicleCount,
                "total", leaveCount + vehicleCount));
    }
}

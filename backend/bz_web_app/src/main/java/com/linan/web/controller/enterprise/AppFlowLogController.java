package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.office.entity.OfficeSupplyRequest;
import com.linan.office.service.IOfficeSupplyRequestService;
import com.linan.system.entity.ApprovalFlowLog;
import com.linan.system.service.IApprovalFlowLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 审批流程进度查询 Controller（用户前台 /api/app/v1/flow-logs）
 *
 * <p>员工查看自己申请单的流程节点（办事进程图）。
 * 带归属校验：仅能查询自己提交的申请（加班/出差/用品申领）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "审批流程进度", description = "用户前台 - 查看我的申请进度")
@RestController
@RequestMapping("/api/app/v1/flow-logs")
public class AppFlowLogController {

    @Autowired
    private IApprovalFlowLogService flowLogService;

    @Autowired
    private IAttOvertimeRequestService overtimeService;

    @Autowired
    private IAttTravelRequestService travelService;

    @Autowired
    private IOfficeSupplyRequestService supplyRequestService;

    /**
     * 查询我的申请单流程节点
     * GET /api/app/v1/flow-logs?bizType=overtime&bizId=1
     */
    @Operation(summary = "查询我的申请流程节点")
    @GetMapping
    public Result<List<ApprovalFlowLog>> list(
            @RequestParam String bizType,
            @RequestParam Long bizId) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 归属校验：仅允许查询本人提交的申请
        boolean owned = switch (bizType) {
            case "overtime" -> {
                AttOvertimeRequest r = overtimeService.getById(bizId);
                yield r != null && userId.equals(r.getUserId());
            }
            case "travel" -> {
                AttTravelRequest r = travelService.getById(bizId);
                yield r != null && userId.equals(r.getUserId());
            }
            case "supply" -> {
                OfficeSupplyRequest r = supplyRequestService.getById(bizId);
                yield r != null && userId.equals(r.getUserId());
            }
            default -> false;
        };
        if (!owned) {
            return Result.error("无权查看该申请");
        }

        LambdaQueryWrapper<ApprovalFlowLog> wrapper = new LambdaQueryWrapper<ApprovalFlowLog>()
                .eq(ApprovalFlowLog::getBizType, bizType)
                .eq(ApprovalFlowLog::getBizId, bizId)
                .orderByAsc(ApprovalFlowLog::getCreateTime);
        return Result.success(flowLogService.list(wrapper));
    }
}

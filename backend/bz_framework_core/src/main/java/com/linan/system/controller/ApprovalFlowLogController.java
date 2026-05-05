package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.ApprovalFlowLog;
import com.linan.system.entity.ApprovalFlowTask;
import com.linan.system.service.ApprovalFlowEngineService;
import com.linan.system.service.IApprovalFlowLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 审批流程进度查询 Controller（管理后台）
 *
 * <p>按业务类型 + 业务ID 返回该申请单的完整流程节点列表（时间正序），
 * 供管理后台在审批/详情页面展示办事进程图。</p>
 *
 * <p>权限：需要 {@code system:flowlog:list} 权限（F 类型菜单，挂「待办中心」下），
 * 防止任意登录用户遍历 bizType/bizId 越权查看他人审批进度（IDOR）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "审批流程进度", description = "查询申请单的流程节点列表")
@RestController
@RequestMapping("/api/system/v1/flow-logs")
@RequiredArgsConstructor
public class ApprovalFlowLogController {

    private final IApprovalFlowLogService flowLogService;

    /** 多级审批引擎（V36） */
    private final ApprovalFlowEngineService flowEngine;

    /**
     * 查询指定申请单的流程节点（时间正序）
     * GET /api/system/v1/flow-logs?bizType=leave&bizId=1
     */
    @Operation(summary = "查询申请单流程节点")
    @ApiResponse(responseCode = "200", description = "流程节点列表（时间正序）")
    @RequirePermission("system:flowlog:list")
    @GetMapping
    public Result<List<ApprovalFlowLog>> list(
            @Parameter(description = "业务类型：leave/overtime/travel/supply/vehicle") @RequestParam String bizType,
            @Parameter(description = "业务ID（申请单ID）") @RequestParam Long bizId) {

        LambdaQueryWrapper<ApprovalFlowLog> wrapper = new LambdaQueryWrapper<ApprovalFlowLog>()
                .eq(ApprovalFlowLog::getBizType, bizType)
                .eq(ApprovalFlowLog::getBizId, bizId)
                .orderByAsc(ApprovalFlowLog::getCreateTime);
        return Result.success(flowLogService.list(wrapper));
    }

    /**
     * 查询指定申请单的审批节点链（V36 多级审批引擎）
     * GET /api/system/v1/flow-tasks?bizType=leave&bizId=1
     *
     * <p>返回模板驱动的节点任务（node_seq 升序，含审批人/意见/时间），
     * 供前端渲染多级审批节点链；与 flow-logs（事件审计流）互补。</p>
     */
    @Operation(summary = "查询申请单审批节点链")
    @ApiResponse(responseCode = "200", description = "审批节点任务列表（node_seq 升序）")
    @RequirePermission("system:flowlog:list")
    @GetMapping("/flow-tasks")
    public Result<List<ApprovalFlowTask>> tasks(
            @Parameter(description = "业务类型：leave/overtime/travel/vehicle/supply") @RequestParam String bizType,
            @Parameter(description = "业务ID（申请单ID）") @RequestParam Long bizId) {
        // 未接入引擎的业务（supply）返回空链，前端回退展示事件流
        return Result.success(flowEngine.listTasks(bizType, bizId));
    }
}

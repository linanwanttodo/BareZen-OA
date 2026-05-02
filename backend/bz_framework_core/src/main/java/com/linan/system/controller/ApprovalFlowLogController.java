package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.api.Result;
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
 * 审批流程进度查询 Controller（管理后台）
 *
 * <p>按业务类型 + 业务ID 返回该申请单的完整流程节点列表（时间正序），
 * 供管理后台在审批/详情页面展示办事进程图。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "审批流程进度", description = "查询申请单的流程节点列表")
@RestController
@RequestMapping("/api/system/v1/flow-logs")
public class ApprovalFlowLogController {

    @Autowired
    private IApprovalFlowLogService flowLogService;

    /**
     * 查询指定申请单的流程节点（时间正序）
     * GET /api/system/v1/flow-logs?bizType=leave&bizId=1
     */
    @Operation(summary = "查询申请单流程节点")
    @GetMapping
    public Result<List<ApprovalFlowLog>> list(
            @RequestParam String bizType,
            @RequestParam Long bizId) {

        LambdaQueryWrapper<ApprovalFlowLog> wrapper = new LambdaQueryWrapper<ApprovalFlowLog>()
                .eq(ApprovalFlowLog::getBizType, bizType)
                .eq(ApprovalFlowLog::getBizId, bizId)
                .orderByAsc(ApprovalFlowLog::getCreateTime);
        return Result.success(flowLogService.list(wrapper));
    }
}

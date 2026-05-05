package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.exception.BusinessException;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.ApprovalFlowTemplate;
import com.linan.system.service.IApprovalFlowTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Set;

/**
 * 审批流程模板 Controller（管理后台）
 *
 * <p>配置每类业务单的审批节点链：同一 biz_type 多行（node_seq 递增）即多级审批。
 * 种子数据默认每类 1 节点（any），管理员在此新增 node_seq 更大的行即变为多级。
 * 模板变更对后续提交生效，已生成的历史节点任务不受影响。</p>
 *
 * <p>权限：{@code system:flow:template:*}（F 类型菜单，挂「审批引擎」下）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "审批流程模板", description = "配置各类业务单的审批节点链")
@RestController
@RequestMapping("/api/system/v1/flow-templates")
@RequiredArgsConstructor
public class ApprovalFlowTemplateController {

    /** 接入审批引擎的业务类型（supply 未接入，不可配置模板） */
    private static final Set<String> SUPPORTED_BIZ_TYPES =
            Set.of("leave", "overtime", "travel", "vehicle");

    private final IApprovalFlowTemplateService templateService;

    /**
     * 分页查询模板节点（可按业务类型过滤）
     * GET /api/system/v1/flow-templates/page?bizType=leave
     */
    @Operation(summary = "分页查询审批模板节点")
    @RequirePermission("system:flow:template:list")
    @GetMapping("/page")
    public Result<PageResult<ApprovalFlowTemplate>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "业务类型：leave/overtime/travel/vehicle") @RequestParam(required = false) String bizType) {

        Page<ApprovalFlowTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ApprovalFlowTemplate> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(ApprovalFlowTemplate::getBizType, bizType);
        }
        wrapper.orderByAsc(ApprovalFlowTemplate::getBizType)
                .orderByAsc(ApprovalFlowTemplate::getNodeSeq);
        IPage<ApprovalFlowTemplate> result = templateService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 新增模板节点（同 bizType + nodeSeq 唯一，禁止重复）
     */
    @Operation(summary = "新增审批模板节点")
    @OperLog(title = "审批流程模板", businessType = 1)
    @RequirePermission("system:flow:template:add")
    @PostMapping
    public Result<Void> save(@RequestBody ApprovalFlowTemplate tpl) {
        validate(tpl);
        long exists = templateService.count(new LambdaQueryWrapper<ApprovalFlowTemplate>()
                .eq(ApprovalFlowTemplate::getBizType, tpl.getBizType())
                .eq(ApprovalFlowTemplate::getNodeSeq, tpl.getNodeSeq()));
        if (exists > 0) {
            return Result.error("该业务类型下节点序号已存在");
        }
        if (tpl.getEnabled() == null) {
            tpl.setEnabled(1);
        }
        boolean ok = templateService.save(tpl);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新模板节点
     */
    @Operation(summary = "修改审批模板节点")
    @OperLog(title = "审批流程模板", businessType = 2)
    @RequirePermission("system:flow:template:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ApprovalFlowTemplate tpl) {
        tpl.setId(id);
        validate(tpl);
        boolean ok = templateService.updateById(tpl);
        return ok ? Result.success(null) : Result.error("模板节点不存在或更新失败");
    }

    /**
     * 删除模板节点（逻辑删除；已删除的同 seq 节点可重新新增）
     */
    @Operation(summary = "删除审批模板节点")
    @OperLog(title = "审批流程模板", businessType = 3)
    @RequirePermission("system:flow:template:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = templateService.removeById(id);
        return ok ? Result.success(null) : Result.error("模板节点不存在");
    }

    /** 基础合法性校验：业务类型受支持、节点序号为正整数、节点名称非空 */
    private void validate(ApprovalFlowTemplate tpl) {
        if (tpl.getBizType() == null || !SUPPORTED_BIZ_TYPES.contains(tpl.getBizType())) {
            throw new BusinessException("不支持的业务类型：" + tpl.getBizType());
        }
        if (tpl.getNodeSeq() == null || tpl.getNodeSeq() < 1) {
            throw new BusinessException("节点序号必须为正整数");
        }
        if (tpl.getNodeName() == null || tpl.getNodeName().isBlank()) {
            throw new BusinessException("节点名称不能为空");
        }
    }
}
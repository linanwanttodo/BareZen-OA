package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.system.service.IApprovalFlowLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 加班申请 Controller
 *
 * <p>流程：员工提交申请（状态0待审批）→ 管理员审批通过/驳回（状态1/2），
 * 待审批状态下申请人可撤销（状态3）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "加班申请", description = "考勤管理 - 加班申请与审批")
@RestController
@RequestMapping("/api/hr/v1/overtimes")
@RequiredArgsConstructor
public class AttOvertimeRequestController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IAttOvertimeRequestService overtimeService;

    private final ApplicationEventPublisher eventPublisher;

    private final IApprovalFlowLogService flowLogService;

    /**
     * 分页查询加班记录
     */
    @RequirePermission("hr:overtime:list")
    @Operation(summary = "分页查询加班记录")
    @GetMapping("/page")
    public Result<PageResult<AttOvertimeRequest>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onlyMine) {

        Page<AttOvertimeRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AttOvertimeRequest> wrapper = new LambdaQueryWrapper<>();

        if (Boolean.TRUE.equals(onlyMine)) {
            wrapper.eq(AttOvertimeRequest::getUserId, AuthInterceptor.getCurrentUserId());
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AttOvertimeRequest::getStatus, status);
        }
        wrapper.orderByDesc(AttOvertimeRequest::getCreateTime);
        IPage<AttOvertimeRequest> result = overtimeService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 提交加班申请
     */
    @Operation(summary = "提交加班申请")
    @OperLog(title = "加班申请", businessType = 1)
    @RequirePermission("hr:overtime:add")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody AttOvertimeRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择加班起止时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        request.setUserId(userId);
        // 后端兜底填真实用户名（JWT 中解析），禁止把 userId 当姓名落库
        if (request.getUserName() == null || request.getUserName().isEmpty()) {
            String username = AuthInterceptor.getCurrentUsername();
            request.setUserName(username != null ? username : "用户" + userId);
        }
        request.setStatus("0");
        boolean ok = overtimeService.save(request);
        if (ok) {
            flowLogService.record("overtime", request.getId(), "提交申请", "0",
                    request.getUserId(), request.getUserName(), null);
        }
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批加班（通过/驳回）
     *
     * <p>原子条件更新（WHERE status='0'）：并发重复审批时仅首个请求生效，
     * 后到者受影响行数为 0 返回"已审批"，避免覆盖首次审批结果。</p>
     */
    @Operation(summary = "审批加班")
    @OperLog(title = "加班申请", businessType = 2)
    @RequirePermission("hr:overtime:approve")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) String remark) {
        AttOvertimeRequest request = overtimeService.getById(id);
        if (request == null) {
            return Result.error("加班记录不存在");
        }
        Long approverId = AuthInterceptor.getCurrentUserId();
        if (approverId != null && approverId.equals(request.getUserId())) {
            return Result.error("不能审批自己的申请");
        }
        // 指定审批人校验（V31）：若申请指定了审批人，仅该审批人可操作
        if (request.getAssigneeId() != null
                && (approverId == null || !approverId.equals(request.getAssigneeId()))) {
            return Result.error("该申请已指定审批人（" + request.getAssigneeName() + "），仅其可审批");
        }
        String targetStatus;
        if ("pass".equals(action)) {
            targetStatus = "1";
        } else if ("reject".equals(action)) {
            targetStatus = "2";
        } else {
            return Result.error("无效的审批动作");
        }

        // 原子条件更新：仅当仍为待审批（status='0'）时生效，并发重复审批后到者受影响行数为 0
        boolean ok = overtimeService.update(new LambdaUpdateWrapper<AttOvertimeRequest>()
                .eq(AttOvertimeRequest::getId, id)
                .eq(AttOvertimeRequest::getStatus, "0")
                .set(AttOvertimeRequest::getStatus, targetStatus)
                .set(AttOvertimeRequest::getApproverId, approverId)
                .set(AttOvertimeRequest::getApproverName, AuthInterceptor.getCurrentUsername())
                .set(AttOvertimeRequest::getApproveTime, LocalDateTime.now().format(FMT))
                .set(AttOvertimeRequest::getApproveRemark, remark));
        if (!ok) {
            return Result.error("该记录已审批，不能重复操作");
        }

        String node = "pass".equals(action) ? "审批通过" : "审批驳回";
        flowLogService.record("overtime", id, node, targetStatus,
                approverId, AuthInterceptor.getCurrentUsername(), remark);
        String result = "pass".equals(action) ? "已通过" : "已驳回";
        eventPublisher.publishEvent(new ApprovalCompletedEvent(
                request.getUserId(),
                "加班审批" + result,
                "您的加班申请（" + request.getStartTime() + " ~ " + request.getEndTime() + "）" + result
                        + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "")));
        return Result.success(null);
    }

    /**
     * 撤销加班申请（仅待审批状态可撤销）
     *
     * <p>原子条件更新：与审批并发时仅一个操作生效，避免状态被并发覆盖。</p>
     */
    @Operation(summary = "撤销加班申请")
    @OperLog(title = "加班申请", businessType = 3)
    @RequirePermission("hr:overtime:edit")
    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        AttOvertimeRequest request = overtimeService.getById(id);
        if (request == null) {
            return Result.error("加班记录不存在");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null && !userId.equals(request.getUserId())) {
            return Result.error("只能撤销自己的申请");
        }
        boolean ok = overtimeService.update(new LambdaUpdateWrapper<AttOvertimeRequest>()
                .eq(AttOvertimeRequest::getId, id)
                .eq(AttOvertimeRequest::getStatus, "0")
                .set(AttOvertimeRequest::getStatus, "3"));
        if (!ok) {
            return Result.error("仅待审批状态的申请可撤销");
        }
        flowLogService.record("overtime", id, "撤销申请", "3",
                userId, AuthInterceptor.getCurrentUsername(), null);
        return Result.success(null);
    }

    /**
     * 删除加班记录
     */
    @Operation(summary = "删除加班记录")
    @OperLog(title = "加班申请", businessType = 3)
    @RequirePermission("hr:overtime:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = overtimeService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

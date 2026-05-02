package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.event.ApprovalCompletedEvent;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.system.service.IApprovalFlowLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 请假审批 Controller
 *
 * <p>流程：员工提交申请（状态0待审批）→ 管理员审批通过/驳回（状态1/2），
 * 待审批状态下申请人可撤销（状态3）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "请假审批", description = "考勤管理 - 请假申请与审批")
@RestController
@RequestMapping("/api/hr/v1/leaves")
public class AttLeaveRequestController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private IAttLeaveRequestService leaveService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private IApprovalFlowLogService flowLogService;

    /**
     * 分页查询请假记录
     * 普通用户仅看自己的；管理员可通过 userId 筛选查看全部
     */
    @RequirePermission("hr:leave:list")
    @Operation(summary = "分页查询请假记录")
    @GetMapping("/page")
    public Result<PageResult<AttLeaveRequest>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean onlyMine) {

        Page<AttLeaveRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AttLeaveRequest> wrapper = new LambdaQueryWrapper<>();

        // 仅查看自己的（用户端/个人视角）；管理员传 userId 或不过滤
        if (Boolean.TRUE.equals(onlyMine)) {
            wrapper.eq(AttLeaveRequest::getUserId, AuthInterceptor.getCurrentUserId());
        } else if (userId != null) {
            wrapper.eq(AttLeaveRequest::getUserId, userId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AttLeaveRequest::getStatus, status);
        }

        wrapper.orderByDesc(AttLeaveRequest::getCreateTime);
        IPage<AttLeaveRequest> result = leaveService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 提交请假申请（申请人自动填充为当前登录用户）
     */
    @Operation(summary = "提交请假申请")
    @OperLog(title = "请假审批", businessType = 1)
    @RequirePermission("hr:leave:add")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody AttLeaveRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择请假起止时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        request.setUserId(userId);
        // 申请人姓名由前端传入（来自用户信息）；后端兜底
        if (request.getUserName() == null || request.getUserName().isEmpty()) {
            request.setUserName(String.valueOf(userId));
        }
        request.setStatus("0");
        boolean ok = leaveService.save(request);
        if (ok) {
            flowLogService.record("leave", request.getId(), "提交申请", "0",
                    request.getUserId(), request.getUserName(), null);
        }
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批请假（通过/驳回）
     */
    @Operation(summary = "审批请假")
    @OperLog(title = "请假审批", businessType = 2)
    @RequirePermission("hr:leave:approve")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) String remark) {
        AttLeaveRequest leave = leaveService.getById(id);
        if (leave == null) {
            return Result.error("请假记录不存在");
        }
        if (!"0".equals(leave.getStatus())) {
            return Result.error("该记录已审批，不能重复操作");
        }

        // 禁止审批人审批自己的申请（避免自审自批）
        Long approverId = AuthInterceptor.getCurrentUserId();
        if (approverId != null && approverId.equals(leave.getUserId())) {
            return Result.error("不能审批自己的申请");
        }

        if ("pass".equals(action)) {
            leave.setStatus("1");
        } else if ("reject".equals(action)) {
            leave.setStatus("2");
        } else {
            return Result.error("无效的审批动作");
        }

        leave.setApproverId(approverId);
        leave.setApproverName(AuthInterceptor.getCurrentUsername());
        leave.setApproveTime(LocalDateTime.now().format(FMT));
        leave.setApproveRemark(remark);

        boolean ok = leaveService.updateById(leave);
        if (ok) {
            // 记录流程节点：审批通过/驳回
            String node = "pass".equals(action) ? "审批通过" : "审批驳回";
            flowLogService.record("leave", leave.getId(), node, leave.getStatus(),
                    approverId, AuthInterceptor.getCurrentUsername(), remark);
            // 审批结果通知申请人（通过/驳回均推送）
            String result = "pass".equals(action) ? "已通过" : "已驳回";
            eventPublisher.publishEvent(new ApprovalCompletedEvent(
                    leave.getUserId(),
                    "请假审批" + result,
                    "您的请假申请（" + leave.getStartTime() + " ~ " + leave.getEndTime() + "）" + result
                            + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "")));
        }
        return ok ? Result.success(null) : Result.error("审批失败");
    }

    /**
     * 撤销请假申请（仅待审批状态可撤销）
     */
    @Operation(summary = "撤销请假申请")
    @OperLog(title = "请假审批", businessType = 3)
    @RequirePermission("hr:leave:edit")
    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        AttLeaveRequest leave = leaveService.getById(id);
        if (leave == null) {
            return Result.error("请假记录不存在");
        }
        if (!"0".equals(leave.getStatus())) {
            return Result.error("仅待审批状态的申请可撤销");
        }
        // 仅本人可撤销自己的申请
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null && !userId.equals(leave.getUserId())) {
            return Result.error("只能撤销自己的申请");
        }
        leave.setStatus("3");
        boolean ok = leaveService.updateById(leave);
        if (ok) {
            flowLogService.record("leave", leave.getId(), "撤销申请", "3",
                    userId, AuthInterceptor.getCurrentUsername(), null);
        }
        return ok ? Result.success(null) : Result.error("撤销失败");
    }

    /**
     * 删除请假记录
     */
    @Operation(summary = "删除请假记录")
    @OperLog(title = "请假审批", businessType = 3)
    @RequirePermission("hr:leave:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = leaveService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

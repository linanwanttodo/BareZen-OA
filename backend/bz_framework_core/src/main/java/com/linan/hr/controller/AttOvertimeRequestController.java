package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.security.PermissionSupport;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.system.service.ApprovalFlowFacade;
import com.linan.system.service.ApprovalNotifyTexts;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /** 审批流程编排门面（V36+：统一事务/引擎/日志/通知） */
    private final ApprovalFlowFacade flowFacade;

    /** 权限辅助工具 */
    private final PermissionSupport permissionSupport;

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
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean onlyMine) {

        Page<AttOvertimeRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AttOvertimeRequest> wrapper = new LambdaQueryWrapper<>();

        // 服务端强制权限校验：仅管理员（持 hr:overtime:audit）可按 userId 筛选，否则强制只看本人
        Long currentUserId = AuthInterceptor.getCurrentUserId();
        boolean hasAuditPerm = permissionSupport.hasPermission(currentUserId, "hr:overtime:audit");
        
        if (hasAuditPerm && userId != null) {
            wrapper.eq(AttOvertimeRequest::getUserId, userId);
        } else {
            wrapper.eq(AttOvertimeRequest::getUserId, currentUserId);
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
        request.setStatus(ApprovalStatus.PENDING.getValue());
        boolean ok = overtimeService.save(request);
        if (ok) {
            // 多级审批引擎门面：启动首节点 + 记录提交申请日志（同一事务）
            flowFacade.submit("overtime", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                    request.getUserId(), request.getUserName());
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
        String approverName = AuthInterceptor.getCurrentUsername();

        // 审批编排统一收口：自审校验 / 引擎推进 / 终态更新 / 日志 / 通知事件
        flowFacade.approve("overtime", id, approverId, approverName, action, remark, new ApprovalFlowFacade.ApprovalBizHandler() {
            @Override
            public Long applicantId() {
                return request.getUserId();
            }

            @Override
            public boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
                return overtimeService.update(new LambdaUpdateWrapper<AttOvertimeRequest>()
                        .eq(AttOvertimeRequest::getId, id)
                        .eq(AttOvertimeRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(AttOvertimeRequest::getStatus, targetStatus)
                        .set(AttOvertimeRequest::getApproverId, approverId)
                        .set(AttOvertimeRequest::getApproverName, approverName)
                        .set(AttOvertimeRequest::getApproveTime, LocalDateTime.now().format(FMT))
                        .set(AttOvertimeRequest::getApproveRemark, remark));
            }

            @Override
            public boolean cancel() {
                return overtimeService.update(new LambdaUpdateWrapper<AttOvertimeRequest>()
                        .eq(AttOvertimeRequest::getId, id)
                        .eq(AttOvertimeRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(AttOvertimeRequest::getStatus, ApprovalStatus.CANCELED.getValue()));
            }

            @Override
            public ApprovalFlowFacade.NotifyText resultNotify(boolean approved, String remark) {
                String res = approved ? "已通过" : "已驳回";
                String content = "您的加班申请（" + request.getStartTime() + " ~ " + request.getEndTime() + "）" + res
                        + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "");
                return ApprovalFlowFacade.NotifyText.of(ApprovalNotifyTexts.resultTitle("加班", approved), content);
            }

            @Override
            public ApprovalFlowFacade.NotifyText nextNotify() {
                return ApprovalFlowFacade.NotifyText.of(ApprovalNotifyTexts.nextNotifyTitle(),
                        "有新的加班申请（" + request.getStartTime() + " ~ " + request.getEndTime() + "）待您审批");
            }
        });
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
        String userName = AuthInterceptor.getCurrentUsername();

        // 撤销编排统一收口：归属校验 / 业务表 0→3 / 引擎节点取消 / 日志（同一事务）
        flowFacade.revoke("overtime", id, userId, userName, new ApprovalFlowFacade.ApprovalBizHandler() {
            @Override
            public Long applicantId() {
                return request.getUserId();
            }

            @Override
            public boolean cancel() {
                return overtimeService.update(new LambdaUpdateWrapper<AttOvertimeRequest>()
                        .eq(AttOvertimeRequest::getId, id)
                        .eq(AttOvertimeRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(AttOvertimeRequest::getStatus, ApprovalStatus.CANCELED.getValue()));
            }
        });
        return Result.success(null);
    }

    /**
     * 修改加班申请（仅本人、仅待审批状态）
     *
     * <p>只允许改业务字段：状态/审批人/审批时间/审批意见由审批流写入，
     * 不得经此接口篡改，故用稀疏实体交给 updateById（MP 默认 NOT_NULL
     * 策略，未赋值字段不进 UPDATE 语句）。</p>
     */
    @Operation(summary = "修改加班申请", description = "修改本人提交且仍处于待审批状态的加班申请，仅业务字段可改")
    @OperLog(title = "加班申请", businessType = 2)
    @RequirePermission("hr:overtime:edit")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AttOvertimeRequest body) {
        AttOvertimeRequest exist = overtimeService.getById(id);
        if (exist == null) {
            return Result.error("加班记录不存在");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null || !userId.equals(exist.getUserId())) {
            return Result.error("仅可修改本人提交的申请");
        }
        if (!ApprovalStatus.PENDING.getValue().equals(exist.getStatus())) {
            return Result.error("仅待审批状态的申请可修改");
        }

        AttOvertimeRequest patch = new AttOvertimeRequest();
        patch.setId(id);
        patch.setStartTime(body.getStartTime());
        patch.setEndTime(body.getEndTime());
        patch.setDurationHours(body.getDurationHours());
        patch.setReason(body.getReason());
        boolean ok = overtimeService.updateById(patch);
        return ok ? Result.success(null) : Result.error("修改失败");
    }

    /**
     * 删除加班记录
     */
    @Operation(summary = "删除加班记录")
    @OperLog(title = "加班申请", businessType = 3)
    @RequirePermission("hr:overtime:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AttOvertimeRequest request = overtimeService.getById(id);
        if (request == null) {
            return Result.error("加班记录不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("仅待审批状态的单据可删除");
        }
        boolean ok = overtimeService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

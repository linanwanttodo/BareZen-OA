package com.linan.hr.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.enums.ApprovalStatus;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.system.service.ApprovalFlowFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 出差申请 Controller
 *
 * <p>流程：员工提交申请（状态0待审批）→ 管理员审批通过/驳回（状态1/2），
 * 待审批状态下申请人可撤销（状态3）。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "出差申请", description = "考勤管理 - 出差申请与审批")
@RestController
@RequestMapping("/api/hr/v1/travels")
@RequiredArgsConstructor
public class AttTravelRequestController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IAttTravelRequestService travelService;

    /** 审批流程编排门面（V36+：统一事务/引擎/日志/通知） */
    private final ApprovalFlowFacade flowFacade;

    /**
     * 分页查询出差记录
     */
    @RequirePermission("hr:travel:list")
    @Operation(summary = "分页查询出差记录")
    @GetMapping("/page")
    public Result<PageResult<AttTravelRequest>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onlyMine) {

        Page<AttTravelRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AttTravelRequest> wrapper = new LambdaQueryWrapper<>();

        if (Boolean.TRUE.equals(onlyMine)) {
            wrapper.eq(AttTravelRequest::getUserId, AuthInterceptor.getCurrentUserId());
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AttTravelRequest::getStatus, status);
        }
        wrapper.orderByDesc(AttTravelRequest::getCreateTime);
        IPage<AttTravelRequest> result = travelService.page(page, wrapper);
        return Result.page(result);
    }

    /**
     * 提交出差申请
     */
    @Operation(summary = "提交出差申请")
    @OperLog(title = "出差申请", businessType = 1)
    @RequirePermission("hr:travel:add")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody AttTravelRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择出差起止时间");
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
        boolean ok = travelService.save(request);
        if (ok) {
            // 多级审批引擎门面：启动首节点 + 记录提交申请日志（同一事务）
            flowFacade.submit("travel", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                    request.getUserId(), request.getUserName());
        }
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批出差（通过/驳回）
     *
     * <p>原子条件更新（WHERE status='0'）：并发重复审批时仅首个请求生效，
     * 后到者受影响行数为 0 返回"已审批"，避免覆盖首次审批结果。</p>
     */
    @Operation(summary = "审批出差")
    @OperLog(title = "出差申请", businessType = 2)
    @RequirePermission("hr:travel:approve")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam String action,
                                @RequestParam(required = false) String remark) {
        AttTravelRequest request = travelService.getById(id);
        if (request == null) {
            return Result.error("出差记录不存在");
        }
        Long approverId = AuthInterceptor.getCurrentUserId();
        String approverName = AuthInterceptor.getCurrentUsername();

        // 审批编排统一收口：自审校验 / 引擎推进 / 终态更新 / 日志 / 通知事件
        flowFacade.approve("travel", id, approverId, approverName, action, remark, new ApprovalFlowFacade.ApprovalBizHandler() {
            @Override
            public Long applicantId() {
                return request.getUserId();
            }

            @Override
            public boolean finish(String targetStatus, Long approverId, String approverName, String remark) {
                return travelService.update(new LambdaUpdateWrapper<AttTravelRequest>()
                        .eq(AttTravelRequest::getId, id)
                        .eq(AttTravelRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(AttTravelRequest::getStatus, targetStatus)
                        .set(AttTravelRequest::getApproverId, approverId)
                        .set(AttTravelRequest::getApproverName, approverName)
                        .set(AttTravelRequest::getApproveTime, LocalDateTime.now().format(FMT))
                        .set(AttTravelRequest::getApproveRemark, remark));
            }

            @Override
            public boolean cancel() {
                return travelService.update(new LambdaUpdateWrapper<AttTravelRequest>()
                        .eq(AttTravelRequest::getId, id)
                        .eq(AttTravelRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(AttTravelRequest::getStatus, ApprovalStatus.CANCELED.getValue()));
            }

            @Override
            public ApprovalFlowFacade.NotifyText resultNotify(boolean approved, String remark) {
                String res = approved ? "已通过" : "已驳回";
                String content = "您的出差申请（" + request.getDestination() + "，" + request.getStartTime() + " ~ "
                        + request.getEndTime() + "）" + res
                        + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "");
                return ApprovalFlowFacade.NotifyText.of("出差审批" + res, content);
            }

            @Override
            public ApprovalFlowFacade.NotifyText nextNotify() {
                return ApprovalFlowFacade.NotifyText.of("新的待审批申请",
                        "有新的出差申请（" + request.getStartTime() + " ~ " + request.getEndTime() + "）待您审批");
            }
        });
        return Result.success(null);
    }

    /**
     * 撤销出差申请（仅待审批状态可撤销）
     *
     * <p>原子条件更新：与审批并发时仅一个操作生效，避免状态被并发覆盖。</p>
     */
    @Operation(summary = "撤销出差申请")
    @OperLog(title = "出差申请", businessType = 3)
    @RequirePermission("hr:travel:edit")
    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        AttTravelRequest request = travelService.getById(id);
        if (request == null) {
            return Result.error("出差记录不存在");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        String userName = AuthInterceptor.getCurrentUsername();

        // 撤销编排统一收口：归属校验 / 业务表 0→3 / 引擎节点取消 / 日志（同一事务）
        flowFacade.revoke("travel", id, userId, userName, new ApprovalFlowFacade.ApprovalBizHandler() {
            @Override
            public Long applicantId() {
                return request.getUserId();
            }

            @Override
            public boolean cancel() {
                return travelService.update(new LambdaUpdateWrapper<AttTravelRequest>()
                        .eq(AttTravelRequest::getId, id)
                        .eq(AttTravelRequest::getStatus, ApprovalStatus.PENDING.getValue())
                        .set(AttTravelRequest::getStatus, ApprovalStatus.CANCELED.getValue()));
            }
        });
        return Result.success(null);
    }

    /**
     * 删除出差记录
     */
    @Operation(summary = "删除出差记录")
    @OperLog(title = "出差申请", businessType = 3)
    @RequirePermission("hr:travel:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AttTravelRequest request = travelService.getById(id);
        if (request == null) {
            return Result.error("出差记录不存在");
        }
        if (!"0".equals(request.getStatus())) {
            return Result.error("仅待审批状态的单据可删除");
        }
        boolean ok = travelService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

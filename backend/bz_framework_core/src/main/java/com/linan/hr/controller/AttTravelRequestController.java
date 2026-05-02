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
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.service.IAttTravelRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
public class AttTravelRequestController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private IAttTravelRequestService travelService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
        if (request.getUserName() == null || request.getUserName().isEmpty()) {
            request.setUserName(String.valueOf(userId));
        }
        request.setStatus("0");
        boolean ok = travelService.save(request);
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批出差（通过/驳回）
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
        if (!"0".equals(request.getStatus())) {
            return Result.error("该记录已审批，不能重复操作");
        }
        Long approverId = AuthInterceptor.getCurrentUserId();
        if (approverId != null && approverId.equals(request.getUserId())) {
            return Result.error("不能审批自己的申请");
        }
        if ("pass".equals(action)) {
            request.setStatus("1");
        } else if ("reject".equals(action)) {
            request.setStatus("2");
        } else {
            return Result.error("无效的审批动作");
        }
        request.setApproverId(approverId);
        request.setApproverName(AuthInterceptor.getCurrentUsername());
        request.setApproveTime(LocalDateTime.now().format(FMT));
        request.setApproveRemark(remark);
        boolean ok = travelService.updateById(request);
        if (ok) {
            String result = "pass".equals(action) ? "已通过" : "已驳回";
            eventPublisher.publishEvent(new ApprovalCompletedEvent(
                    request.getUserId(),
                    "出差审批" + result,
                    "您的出差申请（" + request.getDestination() + "，" + request.getStartTime() + " ~ " + request.getEndTime() + "）" + result
                            + (remark != null && !remark.isEmpty() ? "，审批意见：" + remark : "")));
        }
        return ok ? Result.success(null) : Result.error("审批失败");
    }

    /**
     * 撤销出差申请（仅待审批状态可撤销）
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
        if (!"0".equals(request.getStatus())) {
            return Result.error("仅待审批状态的申请可撤销");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId != null && !userId.equals(request.getUserId())) {
            return Result.error("只能撤销自己的申请");
        }
        request.setStatus("3");
        boolean ok = travelService.updateById(request);
        return ok ? Result.success(null) : Result.error("撤销失败");
    }

    /**
     * 删除出差记录
     */
    @Operation(summary = "删除出差记录")
    @OperLog(title = "出差申请", businessType = 3)
    @RequirePermission("hr:travel:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = travelService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

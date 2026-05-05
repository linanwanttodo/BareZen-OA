package com.linan.web.controller.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.hr.entity.AttLeaveRequest;
import com.linan.hr.entity.AttOvertimeRequest;
import com.linan.hr.entity.AttTravelRequest;
import com.linan.hr.entity.HrWorkLog;
import com.linan.hr.service.IAttLeaveRequestService;
import com.linan.hr.service.IAttOvertimeRequestService;
import com.linan.hr.service.IAttTravelRequestService;
import com.linan.hr.service.IHrWorkLogService;
import com.linan.system.service.ApprovalFlowEngineService;
import com.linan.system.service.IApprovalFlowLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 员工自助申请 Controller（用户前台 /api/app/v1）
 *
 * <p>普通员工通过用户前台自助提交加班/出差申请、查询我的申请、撤销待审批申请。
 * 与管理后台的审批接口（/api/hr/v1/overtimes 等）分离：这里仅需登录即可使用，
 * 自动填充当前登录用户为申请人。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "员工自助申请", description = "用户前台 - 加班/出差自助申请")
@RestController
@RequestMapping("/api/app/v1")
@RequiredArgsConstructor
public class AppAttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AppAttendanceController.class);

    private final IAttOvertimeRequestService overtimeService;

    private final IAttTravelRequestService travelService;

    private final IAttLeaveRequestService leaveService;

    private final IHrWorkLogService workLogService;

    private final IApprovalFlowLogService flowLogService;

    /** 多级审批引擎（V36：模板驱动节点链） */
    private final ApprovalFlowEngineService flowEngine;

    // ==================== 请假申请 ====================

    /**
     * 我的请假申请列表
     * GET /api/app/v1/leaves
     */
    @Operation(summary = "我的请假申请列表")
    @GetMapping("/leaves")
    public Result<PageResult<AttLeaveRequest>> myLeaves(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            LambdaQueryWrapper<AttLeaveRequest> wrapper = new LambdaQueryWrapper<AttLeaveRequest>()
                    .eq(AttLeaveRequest::getUserId, userId);
            if (status != null && !status.isEmpty()) {
                wrapper.eq(AttLeaveRequest::getStatus, status);
            }
            wrapper.orderByDesc(AttLeaveRequest::getCreateTime);
            IPage<AttLeaveRequest> result = leaveService.page(new Page<>(page, pageSize), wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询我的请假申请失败, userId={}", userId, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 提交请假申请
     * POST /api/app/v1/leaves
     */
    @Operation(summary = "提交请假申请")
    @PostMapping("/leaves")
    public Result<Void> submitLeave(@RequestBody AttLeaveRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (request.getLeaveType() == null || request.getLeaveType().isBlank()) {
            return Result.error("请选择请假类型");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择请假起止时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        try {
            // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
            request.setUserId(userId);
            request.setUserName(AuthInterceptor.getCurrentUsername());
            request.setStatus("0");
            boolean ok = leaveService.save(request);
            if (ok) {
                // 多级审批引擎：按模板生成首节点任务
                flowEngine.start("leave", request.getId(), request.getAssigneeId(), request.getAssigneeName());
                flowLogService.record("leave", request.getId(), "提交申请", "0",
                        userId, request.getUserName(), null);
            }
            return ok ? Result.success(null) : Result.error("提交失败");
        } catch (Exception e) {
            log.error("提交请假申请失败, userId={}", userId, e);
            return Result.error("提交失败");
        }
    }

    /**
     * 撤销我的请假申请
     * PUT /api/app/v1/leaves/{id}/revoke
     *
     * <p>原子条件更新：与审批并发（如管理员同时审批）时仅一个操作生效，
     * 撤销或审批先到先得，避免状态被并发覆盖。</p>
     */
    @Operation(summary = "撤销请假申请")
    @OperLog(title = "请假申请", businessType = 3)
    @PutMapping("/leaves/{id}/revoke")
    public Result<Void> revokeLeave(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        AttLeaveRequest request = leaveService.getById(id);
        if (request == null) {
            return Result.error("记录不存在");
        }
        if (!userId.equals(request.getUserId())) {
            return Result.error("只能撤销自己的申请");
        }
        boolean ok = leaveService.update(new LambdaUpdateWrapper<AttLeaveRequest>()
                .eq(AttLeaveRequest::getId, id)
                .eq(AttLeaveRequest::getStatus, "0")
                .set(AttLeaveRequest::getStatus, "3"));
        if (!ok) {
            // 失败也留痕：记录单据当前实际状态（可能审批已把状态改走），保证撤销失败可审计
            flowLogService.record("leave", request.getId(), "撤销失败",
                    request.getStatus(), userId, request.getUserName(), "仅待审批状态的申请可撤销");
            return Result.error("仅待审批状态的申请可撤销");
        }
        // 引擎：待审节点置取消（与业务表状态联动）
        flowEngine.revoke("leave", request.getId());
        flowLogService.record("leave", request.getId(), "撤销申请", "3",
                userId, request.getUserName(), null);
        return Result.success(null);
    }

    // ==================== 加班申请 ====================

    /**
     * 我的加班申请列表
     * GET /api/app/v1/overtimes
     */
    @Operation(summary = "我的加班申请列表")
    @GetMapping("/overtimes")
    public Result<PageResult<AttOvertimeRequest>> myOvertimes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            LambdaQueryWrapper<AttOvertimeRequest> wrapper = new LambdaQueryWrapper<AttOvertimeRequest>()
                    .eq(AttOvertimeRequest::getUserId, userId);
            if (status != null && !status.isEmpty()) {
                wrapper.eq(AttOvertimeRequest::getStatus, status);
            }
            wrapper.orderByDesc(AttOvertimeRequest::getCreateTime);
            IPage<AttOvertimeRequest> result = overtimeService.page(new Page<>(page, pageSize), wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询我的加班申请失败, userId={}", userId, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 提交加班申请
     * POST /api/app/v1/overtimes
     */
    @Operation(summary = "提交加班申请")
    @PostMapping("/overtimes")
    public Result<Void> submitOvertime(@RequestBody AttOvertimeRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择加班起止时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        try {
            // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
            request.setUserId(userId);
            request.setUserName(AuthInterceptor.getCurrentUsername());
            request.setStatus("0");
            boolean ok = overtimeService.save(request);
            if (ok) {
                // 多级审批引擎：按模板生成首节点任务
                flowEngine.start("overtime", request.getId(), request.getAssigneeId(), request.getAssigneeName());
                flowLogService.record("overtime", request.getId(), "提交申请", "0",
                        userId, request.getUserName(), null);
            }
            return ok ? Result.success(null) : Result.error("提交失败");
        } catch (Exception e) {
            log.error("提交加班申请失败, userId={}", userId, e);
            return Result.error("提交失败");
        }
    }

    /**
     * 撤销我的加班申请
     * PUT /api/app/v1/overtimes/{id}/revoke
     *
     * <p>原子条件更新：与审批并发（如管理员同时审批）时仅一个操作生效，
     * 撤销或审批先到先得，避免状态被并发覆盖。</p>
     */
    @Operation(summary = "撤销加班申请")
    @OperLog(title = "加班申请", businessType = 3)
    @PutMapping("/overtimes/{id}/revoke")
    public Result<Void> revokeOvertime(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        AttOvertimeRequest request = overtimeService.getById(id);
        if (request == null) {
            return Result.error("记录不存在");
        }
        if (!userId.equals(request.getUserId())) {
            return Result.error("只能撤销自己的申请");
        }
        boolean ok = overtimeService.update(new LambdaUpdateWrapper<AttOvertimeRequest>()
                .eq(AttOvertimeRequest::getId, id)
                .eq(AttOvertimeRequest::getStatus, "0")
                .set(AttOvertimeRequest::getStatus, "3"));
        if (!ok) {
            // 失败也留痕：记录单据当前实际状态（可能审批已把状态改走），保证撤销失败可审计
            flowLogService.record("overtime", request.getId(), "撤销失败",
                    request.getStatus(), userId, request.getUserName(), "仅待审批状态的申请可撤销");
            return Result.error("仅待审批状态的申请可撤销");
        }
        // 引擎：待审节点置取消（与业务表状态联动）
        flowEngine.revoke("overtime", request.getId());
        flowLogService.record("overtime", request.getId(), "撤销申请", "3",
                userId, request.getUserName(), null);
        return Result.success(null);
    }

    // ==================== 出差申请 ====================

    /**
     * 我的出差申请列表
     * GET /api/app/v1/travels
     */
    @Operation(summary = "我的出差申请列表")
    @GetMapping("/travels")
    public Result<PageResult<AttTravelRequest>> myTravels(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            LambdaQueryWrapper<AttTravelRequest> wrapper = new LambdaQueryWrapper<AttTravelRequest>()
                    .eq(AttTravelRequest::getUserId, userId);
            if (status != null && !status.isEmpty()) {
                wrapper.eq(AttTravelRequest::getStatus, status);
            }
            wrapper.orderByDesc(AttTravelRequest::getCreateTime);
            IPage<AttTravelRequest> result = travelService.page(new Page<>(page, pageSize), wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询我的出差申请失败, userId={}", userId, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 提交出差申请
     * POST /api/app/v1/travels
     */
    @Operation(summary = "提交出差申请")
    @PostMapping("/travels")
    public Result<Void> submitTravel(@RequestBody AttTravelRequest request) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return Result.error("请选择出差起止时间");
        }
        if (request.getStartTime().compareTo(request.getEndTime()) >= 0) {
            return Result.error("结束时间必须晚于开始时间");
        }
        try {
            // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
            request.setUserId(userId);
            request.setUserName(AuthInterceptor.getCurrentUsername());
            request.setStatus("0");
            boolean ok = travelService.save(request);
            if (ok) {
                // 多级审批引擎：按模板生成首节点任务
                flowEngine.start("travel", request.getId(), request.getAssigneeId(), request.getAssigneeName());
                flowLogService.record("travel", request.getId(), "提交申请", "0",
                        userId, request.getUserName(), null);
            }
            return ok ? Result.success(null) : Result.error("提交失败");
        } catch (Exception e) {
            log.error("提交出差申请失败, userId={}", userId, e);
            return Result.error("提交失败");
        }
    }

    /**
     * 撤销我的出差申请
     * PUT /api/app/v1/travels/{id}/revoke
     *
     * <p>原子条件更新：与审批并发时仅一个操作生效，避免状态被并发覆盖。</p>
     */
    @Operation(summary = "撤销出差申请")
    @OperLog(title = "出差申请", businessType = 3)
    @PutMapping("/travels/{id}/revoke")
    public Result<Void> revokeTravel(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        AttTravelRequest request = travelService.getById(id);
        if (request == null) {
            return Result.error("记录不存在");
        }
        if (!userId.equals(request.getUserId())) {
            return Result.error("只能撤销自己的申请");
        }
        boolean ok = travelService.update(new LambdaUpdateWrapper<AttTravelRequest>()
                .eq(AttTravelRequest::getId, id)
                .eq(AttTravelRequest::getStatus, "0")
                .set(AttTravelRequest::getStatus, "3"));
        if (!ok) {
            // 失败也留痕：记录单据当前实际状态（可能审批已把状态改走），保证撤销失败可审计
            flowLogService.record("travel", request.getId(), "撤销失败",
                    request.getStatus(), userId, request.getUserName(), "仅待审批状态的申请可撤销");
            return Result.error("仅待审批状态的申请可撤销");
        }
        // 引擎：待审节点置取消（与业务表状态联动）
        flowEngine.revoke("travel", request.getId());
        flowLogService.record("travel", request.getId(), "撤销申请", "3",
                userId, request.getUserName(), null);
        return Result.success(null);
    }

    // ==================== 工作日志 ====================

    /**
     * 我的工作日志列表
     * GET /api/app/v1/worklogs
     */
    @Operation(summary = "我的工作日志列表")
    @GetMapping("/worklogs")
    public Result<PageResult<HrWorkLog>> myWorkLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String logType) {

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        try {
            LambdaQueryWrapper<HrWorkLog> wrapper = new LambdaQueryWrapper<HrWorkLog>()
                    .eq(HrWorkLog::getUserId, userId);
            if (logType != null && !logType.isEmpty()) {
                wrapper.eq(HrWorkLog::getLogType, logType);
            }
            wrapper.orderByDesc(HrWorkLog::getLogDate);
            IPage<HrWorkLog> result = workLogService.page(new Page<>(page, pageSize), wrapper);
            return Result.page(result);
        } catch (Exception e) {
            log.error("查询我的工作日志失败, userId={}", userId, e);
            return Result.error("查询失败");
        }
    }

    /**
     * 提交工作日志
     * POST /api/app/v1/worklogs
     */
    @Operation(summary = "提交工作日志")
    @PostMapping("/worklogs")
    public Result<Void> submitWorkLog(@RequestBody HrWorkLog workLog) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        if (workLog.getContent() == null || workLog.getContent().isBlank()) {
            return Result.error("日志内容不能为空");
        }
        try {
            workLog.setUserId(userId);
            boolean ok = workLogService.save(workLog);
            return ok ? Result.success(null) : Result.error("提交失败");
        } catch (Exception e) {
            log.error("提交工作日志失败, userId={}", userId, e);
            return Result.error("提交失败");
        }
    }
}

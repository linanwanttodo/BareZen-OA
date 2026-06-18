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
import com.linan.system.service.ApprovalFlowFacade;
import com.linan.core.common.api.PageParamGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
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

    private final IAttOvertimeRequestService overtimeService;

    private final IAttTravelRequestService travelService;

    private final IAttLeaveRequestService leaveService;

    private final IHrWorkLogService workLogService;

    /** 审批流程编排门面（统一 submit/revoke 编排并保证事务一致） */
    private final ApprovalFlowFacade flowFacade;

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

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<AttLeaveRequest> wrapper = new LambdaQueryWrapper<AttLeaveRequest>()
                .eq(AttLeaveRequest::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AttLeaveRequest::getStatus, status);
        }
        wrapper.orderByDesc(AttLeaveRequest::getCreateTime);
        IPage<AttLeaveRequest> result = leaveService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 提交请假申请
     * POST /api/app/v1/leaves
     */
    @Operation(summary = "提交请假申请")
    @PostMapping("/leaves")
    @Transactional(rollbackFor = Exception.class)
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
        
        // 强制清空客户端可能预置的敏感字段（防主键冲突、审批字段污染）
        request.setId(null);
        request.setApproverId(null);
        request.setApproverName(null);
        request.setApproveTime(null);
        request.setApproveRemark(null);
        
        // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
        request.setUserId(userId);
        request.setUserName(AuthInterceptor.getCurrentUsername());
        request.setStatus("0");
        
        boolean ok = leaveService.save(request);
        if (!ok) {
            return Result.error("提交失败");
        }
        
        try {
            // 审批编排统一收口：启动引擎首节点 + 记录提交日志（同一事务）
            flowFacade.submit("leave", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                    userId, request.getUserName());
        } catch (Exception e) {
            // submit 抛异常时整个事务回滚（包括已保存的请假记录）
            throw new RuntimeException("启动审批流程失败: " + e.getMessage(), e);
        }
        
        return Result.success(null);
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
        // 审批编排统一收口：本人校验 / 业务表 0→3 原子更新 / 引擎节点取消 / 撤销日志（同一事务）
        flowFacade.revoke("leave", id, userId, request.getUserName(),
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return request.getUserId();
                    }

                    @Override
                    public boolean cancel() {
                        return leaveService.update(new LambdaUpdateWrapper<AttLeaveRequest>()
                                .eq(AttLeaveRequest::getId, id)
                                .eq(AttLeaveRequest::getStatus, "0")
                                .set(AttLeaveRequest::getStatus, "3"));
                    }

                    @Override
                    public String currentStatus() {
                        return request.getStatus();
                    }
                });
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

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<AttOvertimeRequest> wrapper = new LambdaQueryWrapper<AttOvertimeRequest>()
                .eq(AttOvertimeRequest::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AttOvertimeRequest::getStatus, status);
        }
        wrapper.orderByDesc(AttOvertimeRequest::getCreateTime);
        IPage<AttOvertimeRequest> result = overtimeService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 提交加班申请
     * POST /api/app/v1/overtimes
     */
    @Operation(summary = "提交加班申请")
    @PostMapping("/overtimes")
    @Transactional(rollbackFor = Exception.class)
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
        // 强制清空客户端可能预置的敏感字段（防主键冲突、审批字段污染）
        request.setId(null);
        request.setApproverId(null);
        request.setApproverName(null);
        request.setApproveTime(null);
        request.setApproveRemark(null);
        // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
        request.setUserId(userId);
        request.setUserName(AuthInterceptor.getCurrentUsername());
        request.setStatus("0");
        boolean ok = overtimeService.save(request);
        if (!ok) {
            return Result.error("提交失败");
        }
        // 审批编排统一收口：启动引擎首节点 + 记录提交日志（同一事务）。
        // submit 抛异常时向上传播，整个事务回滚（包括已保存的加班记录）
        flowFacade.submit("overtime", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                userId, request.getUserName());
        return Result.success(null);
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
        // 审批编排统一收口：本人校验 / 业务表 0→3 原子更新 / 引擎节点取消 / 撤销日志（同一事务）
        flowFacade.revoke("overtime", id, userId, request.getUserName(),
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return request.getUserId();
                    }

                    @Override
                    public boolean cancel() {
                        return overtimeService.update(new LambdaUpdateWrapper<AttOvertimeRequest>()
                                .eq(AttOvertimeRequest::getId, id)
                                .eq(AttOvertimeRequest::getStatus, "0")
                                .set(AttOvertimeRequest::getStatus, "3"));
                    }

                    @Override
                    public String currentStatus() {
                        return request.getStatus();
                    }
                });
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

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<AttTravelRequest> wrapper = new LambdaQueryWrapper<AttTravelRequest>()
                .eq(AttTravelRequest::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AttTravelRequest::getStatus, status);
        }
        wrapper.orderByDesc(AttTravelRequest::getCreateTime);
        IPage<AttTravelRequest> result = travelService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
    }

    /**
     * 提交出差申请
     * POST /api/app/v1/travels
     */
    @Operation(summary = "提交出差申请")
    @PostMapping("/travels")
    @Transactional(rollbackFor = Exception.class)
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
        // 强制清空客户端可能预置的敏感字段（防主键冲突、审批字段污染）
        request.setId(null);
        request.setApproverId(null);
        request.setApproverName(null);
        request.setApproveTime(null);
        request.setApproveRemark(null);
        // 身份字段服务端强制覆盖，不信任请求体（防身份伪造）
        request.setUserId(userId);
        request.setUserName(AuthInterceptor.getCurrentUsername());
        request.setStatus("0");
        boolean ok = travelService.save(request);
        if (!ok) {
            return Result.error("提交失败");
        }
        // 审批编排统一收口：启动引擎首节点 + 记录提交日志（同一事务）。
        // submit 抛异常时向上传播，整个事务回滚（包括已保存的出差记录）
        flowFacade.submit("travel", request.getId(), request.getAssigneeId(), request.getAssigneeName(),
                userId, request.getUserName());
        return Result.success(null);
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
        // 审批编排统一收口：本人/ 业务表 0→3 原子更新 / 引擎节点取消 / 撤销日志（同一事务）
        flowFacade.revoke("travel", id, userId, request.getUserName(),
                new ApprovalFlowFacade.ApprovalBizHandler() {
                    @Override
                    public Long applicantId() {
                        return request.getUserId();
                    }

                    @Override
                    public boolean cancel() {
                        return travelService.update(new LambdaUpdateWrapper<AttTravelRequest>()
                                .eq(AttTravelRequest::getId, id)
                                .eq(AttTravelRequest::getStatus, "0")
                                .set(AttTravelRequest::getStatus, "3"));
                    }

                    @Override
                    public String currentStatus() {
                        return request.getStatus();
                    }
                });
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

        // 分页参数钳制
        PageParamGuard.validate(page, pageSize);

        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        LambdaQueryWrapper<HrWorkLog> wrapper = new LambdaQueryWrapper<HrWorkLog>()
                .eq(HrWorkLog::getUserId, userId);
        if (logType != null && !logType.isEmpty()) {
            wrapper.eq(HrWorkLog::getLogType, logType);
        }
        wrapper.orderByDesc(HrWorkLog::getLogDate);
        IPage<HrWorkLog> result = workLogService.page(new Page<>(page, pageSize), wrapper);
        return Result.page(result);
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
        // 字段白名单：防止 mass assignment 伪造批阅状态（reviewerName/reviewTime 等）
        HrWorkLog entity = new HrWorkLog();
        entity.setUserId(userId);
        entity.setLogType(workLog.getLogType());
        entity.setLogDate(workLog.getLogDate());
        entity.setContent(workLog.getContent());
        boolean ok = workLogService.save(entity);
        return ok ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 更新工作日志
     * PUT /api/app/v1/worklogs
     */
    @Operation(summary = "更新工作日志")
    @PutMapping("/worklogs")
    @OperLog(title = "工作日志", businessType = 2)
    public Result<Void> updateWorkLog(@RequestBody HrWorkLog workLog) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        HrWorkLog existing = workLogService.getById(workLog.getId());
        if (existing == null) {
            return Result.error("日志不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            return Result.error("只能修改自己的日志");
        }
        if (existing.getReviewerName() != null) {
            return Result.error("已批阅的日志不能修改");
        }
        if (workLog.getContent() == null || workLog.getContent().isBlank()) {
            return Result.error("日志内容不能为空");
        }
        existing.setContent(workLog.getContent());
        boolean ok = workLogService.updateById(existing);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除工作日志
     * DELETE /api/app/v1/worklogs/{id}
     */
    @Operation(summary = "删除工作日志")
    @DeleteMapping("/worklogs/{id}")
    @OperLog(title = "工作日志", businessType = 3)
    public Result<Void> deleteWorkLog(@PathVariable Long id) {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        HrWorkLog existing = workLogService.getById(id);
        if (existing == null) {
            return Result.error("日志不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            return Result.error("只能删除自己的日志");
        }
        boolean ok = workLogService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}

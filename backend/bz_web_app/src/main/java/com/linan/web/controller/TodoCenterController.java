package com.linan.web.controller;

import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.web.service.TodoAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * 统一待办中心 Controller
 *
 * <p>聚合各业务模块中"待我审批"的事项。V36+ 多级审批引擎 + V46 用品申领接入后，
 * 请假/加班/出差/用车/用品申领 5 类均以 approval_flow_task 当前待审节点为准
 * （指定审批人节点仅本人可见），聚合与数据库分页下沉至 {@link TodoAggregationService}。</p>
 *
 * @author BareZen Team
 */
@Tag(name = "统一待办中心", description = "聚合各模块待审批事项")
@RestController
@RequestMapping("/api/system/v1/todo-center")
@RequiredArgsConstructor
public class TodoCenterController {

    private final TodoAggregationService todoService;

    /**
     * 分页聚合查询待办事项（引擎待审节点 5 类，数据库分页）
     */
    @RequirePermission("system:todo:list")
    @Operation(summary = "分页查询统一待办")
    @GetMapping("/page")
    public Result<PageResult<TodoAggregationService.TodoItem>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {

        // 参数钳制：pageNum 非法或 pageSize 越界直接拒绝，防数据库分页负数/超大扫描
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            return Result.error("分页参数不合法");
        }
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(todoService.page(userId, pageNum, pageSize, type, keyword));
    }

    /**
     * 待办分类统计（引擎待审节点 5 类）
     */
    @RequirePermission("system:todo:list")
    @Operation(summary = "待办分类统计")
    @GetMapping("/summary")
    public Result<Map<String, Long>> summary() {
        Long userId = AuthInterceptor.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(todoService.summary(userId));
    }
}
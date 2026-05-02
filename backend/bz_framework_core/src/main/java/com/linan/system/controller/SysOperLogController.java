package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.common.controller.BaseController;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysOperLog;
import com.linan.system.service.ISysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "操作日志", description = "操作日志查询与清理")
@RestController
@RequestMapping("/api/system/v1/operlogs")
public class SysOperLogController extends BaseController<SysOperLog, ISysOperLogService> {
    @Autowired private ISysOperLogService operLogService;
    @Override protected ISysOperLogService getService() { return operLogService; }

    @RequirePermission("monitor:operlog:list")
    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @Override
    public Result<PageResult<SysOperLog>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return super.list(pageNum, pageSize);
    }

    @RequirePermission("monitor:operlog:list")
    @Operation(summary = "查询日志详情")
    @GetMapping("/{id}")
    @Override
    public Result<SysOperLog> getById(@PathVariable Long id) {
        return super.getById(id);
    }

    @RequirePermission("monitor:operlog:delete")
    @Operation(summary = "删除操作日志")
    @DeleteMapping("/{id}")
    @Override
    public Result<Void> delete(@PathVariable Long id) {
        return super.delete(id);
    }

    @Operation(summary = "批量删除操作日志")
    @OperLog(title = "操作日志", businessType = 3)
    @RequirePermission("monitor:operlog:delete")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        boolean ok = operLogService.removeByIds(ids);
        return ok ? Result.success(null) : Result.error("删除失败");
    }

    /**
     * 清空指定时间前的操作日志
     * DELETE /api/system/v1/operlogs/clean?beforeDays=30
     */
    @Operation(summary = "清空指定时间前的操作日志")
    @OperLog(title = "操作日志", businessType = 8)
    @RequirePermission("monitor:operlog:delete")
    @DeleteMapping("/clean")
    public Result<Void> clean(@Parameter(description = "清除多少天前的日志") @RequestParam(defaultValue = "30") int beforeDays) {
        LocalDateTime before = LocalDateTime.now().minusDays(beforeDays);
        boolean ok = operLogService.remove(
                new LambdaQueryWrapper<SysOperLog>()
                        .lt(SysOperLog::getCreateTime, before));
        return ok ? Result.success(null) : Result.error("清空失败");
    }
}

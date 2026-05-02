package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysJobLog;
import com.linan.system.mapper.SysJobLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 定时任务日志 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "任务日志", description = "系统监控 - 定时任务执行日志")
@RestController
@RequestMapping("/api/system/v1/job-logs")
public class SysJobLogController {

    @Autowired
    private SysJobLogMapper jobLogMapper;

    @RequirePermission("monitor:job:list")
    @Operation(summary = "分页查询任务日志")
    @GetMapping("/page")
    public Result<PageResult<SysJobLog>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String status) {

        Page<SysJobLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(SysJobLog::getJobId, jobId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysJobLog::getStatus, status);
        }
        wrapper.orderByDesc(SysJobLog::getStartTime);
        // 统一分页返回结构（list 字段），与前端 PageData 类型约定一致
        return Result.page(jobLogMapper.selectPage(page, wrapper));
    }
}

package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysLogininfor;
import com.linan.system.service.ISysLogininforService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/v1/logininfor")
public class SysLogininforController {

    @Autowired
    private ISysLogininforService logininforService;

    @RequirePermission("monitor:logininfor:list")
    @GetMapping("/page")
    public Result<PageResult<SysLogininfor>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String status) {
        Page<SysLogininfor> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLogininfor> wrapper = new LambdaQueryWrapper<>();
        if (userName != null && !userName.isEmpty()) {
            wrapper.like(SysLogininfor::getUserName, userName);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysLogininfor::getStatus, status);
        }
        wrapper.orderByDesc(SysLogininfor::getLoginTime);
        return Result.page(logininforService.page(page, wrapper));
    }

    @OperLog(title = "登录日志", businessType = 8)
    @RequirePermission("monitor:logininfor:delete")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        logininforService.remove(new LambdaQueryWrapper<>());
        return Result.success(null);
    }
}

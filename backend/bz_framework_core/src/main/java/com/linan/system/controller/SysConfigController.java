package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysConfig;
import com.linan.system.service.ISysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 系统配置 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "系统配置", description = "系统参数配置管理")
@RestController
@RequestMapping("/api/system/v1/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final ISysConfigService configService;

    /**
     * 分页查询配置列表
     */
    @RequirePermission("system:config:list")
    @Operation(summary = "分页查询配置列表")
    @GetMapping("/page")
    public Result<PageResult<SysConfig>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) String configKey) {

        Page<SysConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (configName != null && !configName.isEmpty()) {
            wrapper.like(SysConfig::getConfigName, configName);
        }
        if (configKey != null && !configKey.isEmpty()) {
            wrapper.like(SysConfig::getConfigKey, configKey);
        }
        wrapper.orderByAsc(SysConfig::getCreateTime);
        return Result.page(configService.page(page, wrapper));
    }

    /**
     * 根据键名获取配置值
     */
    @RequirePermission("system:config:list")
    @Operation(summary = "根据键名获取配置值")
    @GetMapping("/{configKey}")
    public Result<String> getConfigValue(@Parameter(description = "配置键名") @PathVariable String configKey) {
        String value = configService.selectConfigByKey(configKey);
        return Result.success(value != null ? value : "");
    }

    /**
     * 修改配置
     */
    @Operation(summary = "修改配置")
    @OperLog(title = "系统配置", businessType = 2)
    @RequirePermission("system:config:edit")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysConfig config) {
        boolean ok = configService.updateConfig(config);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 清除缓存
     */
    @RequirePermission("system:config:edit")
    @Operation(summary = "清除指定配置缓存")
    @DeleteMapping("/cache/{configKey}")
    public Result<Void> clearCache(@Parameter(description = "配置键名") @PathVariable String configKey) {
        configService.clearCache(configKey);
        return Result.success(null);
    }

    /**
     * 清除全部缓存
     */
    @RequirePermission("system:config:edit")
    @Operation(summary = "清除全部配置缓存")
    @DeleteMapping("/cache/all")
    public Result<Void> clearAllCache() {
        configService.clearAllCache();
        return Result.success(null);
    }
}

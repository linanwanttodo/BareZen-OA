package com.linan.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linan.core.common.annotation.OperLog;
import com.linan.core.common.api.PageResult;
import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.system.entity.SysConfig;
import com.linan.system.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置 Controller
 *
 * @author BareZen Team
 */
@Tag(name = "系统配置", description = "系统参数配置管理接口")
@RestController
@RequestMapping("/api/system/v1/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final ISysConfigService configService;

    /**
     * 分页查询配置列表
     */
    @Operation(summary = "分页查询配置列表", description = "分页查询系统配置参数，支持按配置名称和配置键模糊搜索")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @RequirePermission("system:config:list")
    @GetMapping("/page")
    public Result<PageResult<SysConfig>> list(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "配置名称（模糊搜索）")
            @RequestParam(required = false) String configName,
            @Parameter(description = "配置键名（模糊搜索）")
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
    @Operation(summary = "根据键名获取配置值", description = "根据配置键名获取对应的配置值")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    @RequirePermission("system:config:list")
    @GetMapping("/{configKey}")
    public Result<String> getConfigValue(
            @Parameter(description = "配置键名", required = true, example = "sys.user.initPassword")
            @PathVariable String configKey) {
        String value = configService.selectConfigByKey(configKey);
        return Result.success(value != null ? value : "");
    }

    /**
     * 修改配置
     */
    @Operation(summary = "修改配置", description = "修改系统配置参数，修改后会自动清除缓存")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
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
    @Operation(summary = "清除指定配置缓存", description = "清除指定配置键的Redis缓存")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清除成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @RequirePermission("system:config:edit")
    @DeleteMapping("/cache/{configKey}")
    public Result<Void> clearCache(
            @Parameter(description = "配置键名", required = true)
            @PathVariable String configKey) {
        configService.clearCache(configKey);
        return Result.success(null);
    }

    /**
     * 清除全部缓存
     */
    @Operation(summary = "清除全部配置缓存", description = "清除所有系统配置的Redis缓存")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清除成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @RequirePermission("system:config:edit")
    @DeleteMapping("/cache/all")
    public Result<Void> clearAllCache() {
        configService.clearAllCache();
        return Result.success(null);
    }
}

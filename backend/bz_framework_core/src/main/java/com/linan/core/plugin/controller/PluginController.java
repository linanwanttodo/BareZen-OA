package com.linan.core.plugin.controller;

import com.linan.core.common.api.Result;
import com.linan.core.plugin.Plugin;
import com.linan.core.plugin.PluginGateState;
import com.linan.core.plugin.PluginManager;
import com.linan.core.plugin.PluginManifest;
import com.linan.core.plugin.PluginStatus;
import com.linan.core.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件管理Controller
 *
 * 提供插件的查询、启动、停止等管理功能
 *
 * @author BareZen Team
 */
@Tag(name = "插件管理", description = "插件管理接口")
@RestController
@RequestMapping("/api/system/v1/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginManager pluginManager;
    private final PluginGateState pluginGateState;

    @RequirePermission("system:plugin:manage")
    @Operation(summary = "查询所有插件")
    @GetMapping
    public Result<List<PluginInfo>> listPlugins() {
        List<PluginInfo> pluginInfos = pluginManager.getAllPlugins().stream()
                .map(this::toPluginInfo)
                .collect(Collectors.toList());
        return Result.success(pluginInfos);
    }

    @RequirePermission("system:plugin:manage")
    @Operation(summary = "查询已启用的插件")
    @GetMapping("/enabled")
    public Result<List<PluginInfo>> listEnabledPlugins() {
        List<PluginInfo> pluginInfos = pluginManager.getEnabledPlugins().stream()
                .map(this::toPluginInfo)
                .collect(Collectors.toList());
        return Result.success(pluginInfos);
    }

    @RequirePermission("system:plugin:manage")
    @Operation(summary = "启动插件")
    @PostMapping("/{pluginId}/start")
    public Result<Void> startPlugin(
            @Parameter(description = "插件ID", required = true)
            @PathVariable String pluginId) {
        try {
            pluginManager.startPlugin(pluginId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("启动插件失败: " + e.getMessage());
        }
    }

    @RequirePermission("system:plugin:manage")
    @Operation(summary = "停止插件")
    @PostMapping("/{pluginId}/stop")
    public Result<Void> stopPlugin(
            @Parameter(description = "插件ID", required = true)
            @PathVariable String pluginId) {
        try {
            pluginManager.stopPlugin(pluginId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("停止插件失败: " + e.getMessage());
        }
    }

    @RequirePermission("system:plugin:manage")
    @Operation(summary = "卸载插件")
    @DeleteMapping("/{pluginId}")
    public Result<Void> unloadPlugin(
            @Parameter(description = "插件ID", required = true)
            @PathVariable String pluginId) {
        try {
            pluginManager.unloadPlugin(pluginId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("卸载插件失败: " + e.getMessage());
        }
    }

    @RequirePermission("system:plugin:manage")
    @Operation(summary = "获取插件详情（清单内容 + 运行状态）")
    @GetMapping("/{pluginId}")
    public Result<PluginDetail> getPlugin(
            @Parameter(description = "插件ID", required = true)
            @PathVariable String pluginId) {
        Plugin plugin = pluginManager.getPlugin(pluginId);
        PluginManifest manifest = pluginManager.getManifest(pluginId);
        if (plugin == null && manifest == null) {
            return Result.error("插件不存在");
        }

        PluginDetail detail = new PluginDetail();
        if (manifest != null) {
            detail.setPluginId(manifest.getPluginId());
            detail.setPluginName(manifest.getPluginName());
            detail.setVersion(manifest.getVersion());
            detail.setDescription(manifest.getDescription());
            detail.setAuthor(manifest.getAuthor());
            detail.setDependencies(manifest.getDependencies());
            detail.setControllers(manifest.getControllers());
            detail.setServices(manifest.getServices());
            detail.setEntities(manifest.getEntities());
            detail.setMigrations(manifest.getMigrations());
            detail.setFrontendRoutes(manifest.getFrontendRoutes());
            detail.setPermissions(manifest.getPermissions());
        } else {
            detail.setPluginId(plugin.getPluginId());
            detail.setPluginName(plugin.getPluginName());
            detail.setVersion(plugin.getVersion());
            detail.setDescription(plugin.getDescription());
            detail.setAuthor(plugin.getAuthor());
            detail.setDependencies(plugin.getDependencies());
        }
        String status = resolveStatus(plugin, pluginId);
        detail.setLoaded(plugin != null);
        detail.setStatus(status);
        detail.setEnabled(PluginStatus.ENABLED.name().equals(status));
        detail.setAvailable(isAvailableStatus(status));
        return Result.success(detail);
    }

    /**
     * 运行状态解析：优先取插件实例状态，实例已被卸载时回退门控状态
     */
    private String resolveStatus(Plugin plugin, String pluginId) {
        if (plugin != null) {
            return plugin.getStatus().name();
        }
        PluginStatus gateStatus = pluginGateState.getStatus(pluginId);
        return gateStatus != null ? gateStatus.name() : PluginStatus.LOADED.name();
    }

    /**
     * 是否可用：与 Plugin.isAvailable 口径一致（ENABLED 或 ACTIVE）
     */
    private boolean isAvailableStatus(String status) {
        return PluginStatus.ACTIVE.name().equals(status)
                || PluginStatus.ENABLED.name().equals(status);
    }

    /**
     * 转换为插件信息DTO
     */
    private PluginInfo toPluginInfo(Plugin plugin) {
        PluginInfo info = new PluginInfo();
        info.setPluginId(plugin.getPluginId());
        info.setPluginName(plugin.getPluginName());
        info.setVersion(plugin.getVersion());
        info.setDescription(plugin.getDescription());
        info.setAuthor(plugin.getAuthor());
        info.setStatus(plugin.getStatus().name());
        info.setEnabled(plugin.isEnabled());
        info.setAvailable(plugin.isAvailable());
        info.setDependencies(plugin.getDependencies());
        return info;
    }

    /**
     * 插件信息DTO
     */
    public static class PluginInfo {
        private String pluginId;
        private String pluginName;
        private String version;
        private String description;
        private String author;
        private String status;
        private boolean enabled;
        private boolean available;
        private List<String> dependencies;

        // Getters and Setters
        public String getPluginId() {
            return pluginId;
        }

        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }

        public String getPluginName() {
            return pluginName;
        }

        public void setPluginName(String pluginName) {
            this.pluginName = pluginName;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }
    }

    /**
     * 插件详情DTO：清单内容 + 运行状态
     */
    public static class PluginDetail {
        private String pluginId;
        private String pluginName;
        private String version;
        private String description;
        private String author;
        private List<String> dependencies;
        private List<String> controllers;
        private List<String> services;
        private List<String> entities;
        private List<String> migrations;
        private List<Map<String, Object>> frontendRoutes;
        private List<String> permissions;
        private String status;
        private boolean loaded;
        private boolean enabled;
        private boolean available;

        public String getPluginId() {
            return pluginId;
        }

        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }

        public String getPluginName() {
            return pluginName;
        }

        public void setPluginName(String pluginName) {
            this.pluginName = pluginName;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }

        public List<String> getControllers() {
            return controllers;
        }

        public void setControllers(List<String> controllers) {
            this.controllers = controllers;
        }

        public List<String> getServices() {
            return services;
        }

        public void setServices(List<String> services) {
            this.services = services;
        }

        public List<String> getEntities() {
            return entities;
        }

        public void setEntities(List<String> entities) {
            this.entities = entities;
        }

        public List<String> getMigrations() {
            return migrations;
        }

        public void setMigrations(List<String> migrations) {
            this.migrations = migrations;
        }

        public List<Map<String, Object>> getFrontendRoutes() {
            return frontendRoutes;
        }

        public void setFrontendRoutes(List<Map<String, Object>> frontendRoutes) {
            this.frontendRoutes = frontendRoutes;
        }

        public List<String> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<String> permissions) {
            this.permissions = permissions;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public void setLoaded(boolean loaded) {
            this.loaded = loaded;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }
}
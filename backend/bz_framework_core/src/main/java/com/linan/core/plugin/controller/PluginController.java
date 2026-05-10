package com.linan.core.plugin.controller;

import com.linan.core.common.api.Result;
import com.linan.core.plugin.Plugin;
import com.linan.core.plugin.PluginManager;
import com.linan.core.plugin.PluginStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @Operation(summary = "查询所有插件")
    @GetMapping
    public Result<List<PluginInfo>> listPlugins() {
        List<PluginInfo> pluginInfos = pluginManager.getAllPlugins().stream()
                .map(this::toPluginInfo)
                .collect(Collectors.toList());
        return Result.success(pluginInfos);
    }

    @Operation(summary = "查询已启用的插件")
    @GetMapping("/enabled")
    public Result<List<PluginInfo>> listEnabledPlugins() {
        List<PluginInfo> pluginInfos = pluginManager.getEnabledPlugins().stream()
                .map(this::toPluginInfo)
                .collect(Collectors.toList());
        return Result.success(pluginInfos);
    }

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

    @Operation(summary = "获取插件详情")
    @GetMapping("/{pluginId}")
    public Result<PluginInfo> getPlugin(
            @Parameter(description = "插件ID", required = true)
            @PathVariable String pluginId) {
        Plugin plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) {
            return Result.error("插件不存在");
        }
        return Result.success(toPluginInfo(plugin));
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
}
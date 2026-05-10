package com.linan.core.plugin;

import java.util.List;

/**
 * 插件接口
 *
 * 所有业务插件都需要实现此接口，遵循统一的插件规范
 *
 * @author BareZen Team
 */
public interface Plugin {

    /**
     * 获取插件ID
     * 必须全局唯一，建议使用反向域名格式，如 com.linan.plugin.salary
     */
    String getPluginId();

    /**
     * 获取插件名称
     */
    String getPluginName();

    /**
     * 获取插件版本
     * 格式：MAJOR.MINOR.PATCH，如 1.0.0
     */
    String getVersion();

    /**
     * 获取插件描述
     */
    String getDescription();

    /**
     * 获取插件作者
     */
    String getAuthor();

    /**
     * 获取依赖的插件ID列表
     * 插件加载时会检查依赖是否满足
     */
    List<String> getDependencies();

    /**
     * 插件初始化
     * 在插件加载时调用，用于初始化插件资源
     */
    void initialize() throws PluginException;

    /**
     * 插件启动
     * 在应用启动完成后调用，用于启动插件功能
     */
    void start() throws PluginException;

    /**
     * 插件停止
     * 在应用关闭时调用，用于清理插件资源
     */
    void stop() throws PluginException;

    /**
     * 获取插件状态
     */
    PluginStatus getStatus();

    /**
     * 设置插件状态
     */
    void setStatus(PluginStatus status);

    /**
     * 插件是否已启用
     */
    default boolean isEnabled() {
        return getStatus() == PluginStatus.ENABLED;
    }

    /**
     * 插件是否可用（已启用且无错误）
     */
    default boolean isAvailable() {
        return getStatus() == PluginStatus.ENABLED || getStatus() == PluginStatus.ACTIVE;
    }
}
package com.linan.core.plugin;

/**
 * 插件状态枚举
 *
 * @author BareZen Team
 */
public enum PluginStatus {

    /**
     * 已加载但未启用
     */
    LOADED,

    /**
     * 已启用
     */
    ENABLED,

    /**
     * 活跃状态（正在运行）
     */
    ACTIVE,

    /**
     * 已禁用
     */
    DISABLED,

    /**
     * 启动失败
     */
    FAILED,

    /**
     * 错误状态
     */
    ERROR
}
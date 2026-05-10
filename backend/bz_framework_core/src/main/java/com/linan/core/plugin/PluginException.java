package com.linan.core.plugin;

/**
 * 插件异常
 *
 * 插件加载、启动、停止过程中抛出的异常
 *
 * @author BareZen Team
 */
public class PluginException extends RuntimeException {

    private final String pluginId;

    public PluginException(String pluginId, String message) {
        super(message);
        this.pluginId = pluginId;
    }

    public PluginException(String pluginId, String message, Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }
}
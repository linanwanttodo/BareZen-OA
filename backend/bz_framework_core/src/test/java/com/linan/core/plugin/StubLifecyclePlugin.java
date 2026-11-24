package com.linan.core.plugin;

/**
 * 插件生命周期测试桩：所有回调为空实现，用于驱动 PluginManager 状态流转
 */
public class StubLifecyclePlugin extends AbstractPlugin {

    private final String pluginId;

    public StubLifecyclePlugin(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public String getPluginName() {
        return "测试插件-" + pluginId;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "测试用插件桩";
    }

    @Override
    public String getAuthor() {
        return "BareZen Team";
    }
}

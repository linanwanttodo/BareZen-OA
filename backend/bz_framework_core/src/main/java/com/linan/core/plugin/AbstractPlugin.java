package com.linan.core.plugin;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 插件抽象基类
 *
 * 提供插件实现的默认行为，简化插件开发
 *
 * @author BareZen Team
 */
@Slf4j
public abstract class AbstractPlugin implements Plugin {

    private PluginStatus status = PluginStatus.LOADED;

    @Override
    public void initialize() throws PluginException {
        log.info("初始化插件: {}", getPluginId());
        // 子类可以覆盖此方法实现自定义初始化逻辑
    }

    @Override
    public void start() throws PluginException {
        log.info("启动插件: {}", getPluginId());
        // 子类可以覆盖此方法实现自定义启动逻辑
    }

    @Override
    public void stop() throws PluginException {
        log.info("停止插件: {}", getPluginId());
        // 子类可以覆盖此方法实现自定义停止逻辑
    }

    @Override
    public PluginStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    @Override
    public List<String> getDependencies() {
        // 默认无依赖
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return String.format("%s v%s [%s]", getPluginName(), getVersion(), getStatus());
    }
}
package com.linan.core.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理器
 *
 * 负责插件的加载、启动、停止、卸载等生命周期管理
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class PluginManager {

    /**
     * 已加载的插件映射表
     * Key: pluginId, Value: Plugin实例
     */
    private final Map<String, Plugin> loadedPlugins = new ConcurrentHashMap<>();

    /**
     * 插件清单映射表
     * Key: pluginId, Value: PluginManifest
     */
    private final Map<String, PluginManifest> pluginManifests = new ConcurrentHashMap<>();

    /**
     * 插件门控状态表（PluginGateAspect 据此拦截非 ACTIVE 插件的接口）
     */
    private final PluginGateState pluginGateState;

    public PluginManager(PluginGateState pluginGateState) {
        this.pluginGateState = pluginGateState;
    }

    /**
     * 加载插件
     *
     * @param plugin 插件实例
     * @throws PluginException 加载失败时抛出
     */
    public void loadPlugin(Plugin plugin) throws PluginException {
        String pluginId = plugin.getPluginId();

        // 快速路径（仅避免重复初始化，非线程安全判据，真正的竞态由下方 putIfAbsent 兜底）
        if (loadedPlugins.containsKey(pluginId)) {
            log.warn("插件已存在: {}", pluginId);
            throw new PluginException(pluginId, "插件已存在");
        }

        log.info("加载插件: {} - {}", pluginId, plugin.getPluginName());

        try {
            // 检查依赖
            checkDependencies(plugin);

            // 初始化插件
            plugin.initialize();
            plugin.setStatus(PluginStatus.LOADED);

        } catch (Exception e) {
            plugin.setStatus(PluginStatus.FAILED);
            pluginGateState.update(pluginId, PluginStatus.FAILED);
            throw new PluginException(pluginId, "插件加载失败: " + e.getMessage(), e);
        }

        // 原子占位替代 containsKey + put 的 check-then-act 竞态：
        // putIfAbsent 返回非 null 说明并发情况下已被其他线程先行加载
        Plugin existing = loadedPlugins.putIfAbsent(pluginId, plugin);
        if (existing != null) {
            log.warn("插件已存在: {}", pluginId);
            throw new PluginException(pluginId, "插件已存在");
        }
        pluginGateState.update(pluginId, PluginStatus.LOADED);
        log.info("插件加载成功: {}", pluginId);
    }

    /**
     * 启动插件
     *
     * @param pluginId 插件ID
     * @throws PluginException 启动失败时抛出
     */
    public void startPlugin(String pluginId) throws PluginException {
        Plugin plugin = loadedPlugins.get(pluginId);
        if (plugin == null) {
            throw new PluginException(pluginId, "插件不存在");
        }

        if (plugin.getStatus() == PluginStatus.ACTIVE) {
            log.warn("插件已处于活跃状态: {}", pluginId);
            return;
        }

        log.info("启动插件: {}", pluginId);

        try {
            plugin.start();
            plugin.setStatus(PluginStatus.ACTIVE);
            pluginGateState.update(pluginId, PluginStatus.ACTIVE);
            log.info("插件启动成功: {}", pluginId);

        } catch (Exception e) {
            plugin.setStatus(PluginStatus.FAILED);
            pluginGateState.update(pluginId, PluginStatus.FAILED);
            throw new PluginException(pluginId, "插件启动失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止插件
     *
     * @param pluginId 插件ID
     * @throws PluginException 停止失败时抛出
     */
    public void stopPlugin(String pluginId) throws PluginException {
        Plugin plugin = loadedPlugins.get(pluginId);
        if (plugin == null) {
            throw new PluginException(pluginId, "插件不存在");
        }

        log.info("停止插件: {}", pluginId);

        try {
            plugin.stop();
            plugin.setStatus(PluginStatus.DISABLED);
            pluginGateState.update(pluginId, PluginStatus.DISABLED);
            log.info("插件停止成功: {}", pluginId);

        } catch (Exception e) {
            plugin.setStatus(PluginStatus.ERROR);
            pluginGateState.update(pluginId, PluginStatus.ERROR);
            throw new PluginException(pluginId, "插件停止失败: " + e.getMessage(), e);
        }
    }

    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @throws PluginException 卸载失败时抛出
     */
    public void unloadPlugin(String pluginId) throws PluginException {
        Plugin plugin = loadedPlugins.get(pluginId);
        if (plugin == null) {
            throw new PluginException(pluginId, "插件不存在");
        }

        log.info("卸载插件: {}", pluginId);

        try {
            // 先停止插件
            if (plugin.getStatus() == PluginStatus.ACTIVE) {
                stopPlugin(pluginId);
            }

            // 移除插件
            loadedPlugins.remove(pluginId);
            pluginManifests.remove(pluginId);

            // 门控条目保留且保持关闭：class 仍在 classpath，卸载后必须继续拒绝服务
            pluginGateState.update(pluginId, PluginStatus.DISABLED);

            log.info("插件卸载成功: {}", pluginId);

        } catch (Exception e) {
            pluginGateState.update(pluginId, PluginStatus.ERROR);
            throw new PluginException(pluginId, "插件卸载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取插件
     *
     * @param pluginId 插件ID
     * @return 插件实例，不存在返回null
     */
    public Plugin getPlugin(String pluginId) {
        return loadedPlugins.get(pluginId);
    }

    /**
     * 获取所有已加载的插件
     *
     * @return 插件列表
     */
    public Collection<Plugin> getAllPlugins() {
        return Collections.unmodifiableCollection(loadedPlugins.values());
    }

    /**
     * 获取所有启用的插件
     *
     * @return 插件列表
     */
    public Collection<Plugin> getEnabledPlugins() {
        return loadedPlugins.values().stream()
                .filter(Plugin::isEnabled)
                .toList();
    }

    /**
     * 检查插件依赖
     *
     * @param plugin 插件实例
     * @throws PluginException 依赖不满足时抛出
     */
    private void checkDependencies(Plugin plugin) throws PluginException {
        List<String> dependencies = plugin.getDependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        for (String depId : dependencies) {
            if (!loadedPlugins.containsKey(depId)) {
                throw new PluginException(plugin.getPluginId(),
                        "依赖插件不存在: " + depId);
            }
        }
    }

    /**
     * 注册插件清单
     *
     * @param manifest 插件清单
     */
    public void registerManifest(PluginManifest manifest) {
        pluginManifests.put(manifest.getPluginId(), manifest);
    }

    /**
     * 获取插件清单
     *
     * @param pluginId 插件ID
     * @return 插件清单，不存在返回null
     */
    public PluginManifest getManifest(String pluginId) {
        return pluginManifests.get(pluginId);
    }

    /**
     * 启动所有已加载的插件
     */
    public void startAllPlugins() {
        log.info("启动所有插件...");
        
        for (Plugin plugin : loadedPlugins.values()) {
            try {
                if (plugin.getStatus() == PluginStatus.LOADED) {
                    startPlugin(plugin.getPluginId());
                }
            } catch (PluginException e) {
                log.error("启动插件失败: {}", plugin.getPluginId(), e);
            }
        }
        
        log.info("所有插件启动完成");
    }

    /**
     * 停止所有插件
     */
    public void stopAllPlugins() {
        log.info("停止所有插件...");
        
        for (Plugin plugin : loadedPlugins.values()) {
            try {
                if (plugin.getStatus() == PluginStatus.ACTIVE) {
                    stopPlugin(plugin.getPluginId());
                }
            } catch (PluginException e) {
                log.error("停止插件失败: {}", plugin.getPluginId(), e);
            }
        }
        
        log.info("所有插件停止完成");
    }
}
package com.linan.core.plugin;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件门控状态表
 *
 * <p>插件 HTTP 接口门控的唯一事实来源：清单注册、插件加载/启动/停止/卸载
 * 都通过 {@link #update} 同步状态，门控切面据此决定放行或拒绝。</p>
 *
 * <p>与 {@link PluginManager} 的 loadedPlugins 分离的原因：
 * unload 会把 Plugin 实例从管理器移除，但门控必须保持关闭（拒绝服务），
 * 因此门控条目独立存在，不随插件实例生命周期销毁。</p>
 *
 * @author BareZen Team
 */
@Component
public class PluginGateState {

    /**
     * Key: pluginId, Value: 门控状态（仅 ACTIVE 表示放行）
     */
    private final Map<String, PluginStatus> states = new ConcurrentHashMap<>();

    /**
     * 注册或更新插件门控状态
     */
    public void update(String pluginId, PluginStatus status) {
        if (pluginId == null || status == null) {
            return;
        }
        states.put(pluginId, status);
    }

    /**
     * 插件是否处于 ACTIVE（门控放行的唯一状态）
     */
    public boolean isActive(String pluginId) {
        return states.get(pluginId) == PluginStatus.ACTIVE;
    }

    /**
     * 获取插件门控状态
     *
     * @return 门控状态，插件未注册返回 null
     */
    public PluginStatus getStatus(String pluginId) {
        return states.get(pluginId);
    }

    /**
     * 获取所有已登记门控的插件ID
     */
    public Set<String> getKnownPluginIds() {
        return Collections.unmodifiableSet(states.keySet());
    }
}

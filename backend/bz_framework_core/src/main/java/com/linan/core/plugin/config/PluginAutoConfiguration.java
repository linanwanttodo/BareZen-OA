package com.linan.core.plugin.config;

import com.linan.core.plugin.Plugin;
import com.linan.core.plugin.PluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.Map;

/**
 * 插件自动配置
 *
 * 在应用启动完成后自动加载和启动所有插件
 *
 * @author BareZen Team
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PluginAutoConfiguration {

    private final PluginManager pluginManager;
    private final ApplicationContext applicationContext;

    /**
     * 应用启动完成后自动加载插件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始加载插件...");

        // 从Spring容器中获取所有Plugin实现
        Map<String, Plugin> plugins = applicationContext.getBeansOfType(Plugin.class);

        if (plugins.isEmpty()) {
            log.info("未发现任何插件");
            return;
        }

        log.info("发现 {} 个插件", plugins.size());

        // 加载所有插件
        for (Plugin plugin : plugins.values()) {
            try {
                pluginManager.loadPlugin(plugin);
                log.info("插件加载成功: {} - {}", plugin.getPluginId(), plugin.getPluginName());
            } catch (Exception e) {
                log.error("插件加载失败: {} - {}", plugin.getPluginId(), plugin.getPluginName(), e);
            }
        }

        // 启动所有已加载的插件
        pluginManager.startAllPlugins();

        log.info("插件加载完成");
    }
}
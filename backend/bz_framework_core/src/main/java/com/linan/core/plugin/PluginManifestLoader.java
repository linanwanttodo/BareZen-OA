package com.linan.core.plugin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 插件清单加载器
 *
 * <p>启动时扫描 classpath:plugins/*.json，解析为 {@link PluginManifest}
 * 并注册到 {@link PluginManager}，同时在 {@link PluginGateState} 中登记门控状态
 * （初始为 LOADED，即关闭；待 {@link PluginManager#startPlugin} 成功后放行）。</p>
 *
 * <p>注册时机为 Bean 初始化（早于内嵌 Web 服务器开始接收请求），保证
 * 门控在插件真正 start 前处于关闭状态，插件接口不因启动窗口期而裸奔。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
public class PluginManifestLoader {

    static final String DEFAULT_LOCATION_PATTERN = "classpath:plugins/*.json";

    private final PluginManager pluginManager;
    private final PluginGateState pluginGateState;
    private final String locationPattern;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    public PluginManifestLoader(PluginManager pluginManager, PluginGateState pluginGateState) {
        this(pluginManager, pluginGateState, DEFAULT_LOCATION_PATTERN);
    }

    PluginManifestLoader(PluginManager pluginManager, PluginGateState pluginGateState,
                         String locationPattern) {
        this.pluginManager = pluginManager;
        this.pluginGateState = pluginGateState;
        this.locationPattern = locationPattern;
    }

    /**
     * 加载并注册所有插件清单；单个清单解析失败不影响其他清单
     */
    @PostConstruct
    public void loadManifests() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources(locationPattern);
        } catch (Exception e) {
            log.error("扫描插件清单失败: {}", locationPattern, e);
            return;
        }

        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                PluginManifest manifest = objectMapper.readValue(in, PluginManifest.class);
                if (manifest.getPluginId() == null || manifest.getPluginId().isBlank()) {
                    log.error("插件清单缺少 pluginId，跳过: {}", resource.getFilename());
                    continue;
                }
                pluginManager.registerManifest(manifest);
                pluginGateState.update(manifest.getPluginId(), PluginStatus.LOADED);
                checkDeclaredControllers(manifest);
                log.info("插件清单已注册: {} ({})", manifest.getPluginId(), resource.getFilename());
            } catch (Exception e) {
                log.error("插件清单解析失败: {}", resource.getFilename(), e);
            }
        }
    }

    /**
     * 启动自检：清单声明的 controller 类不存在时打 WARN
     * （插件代码未随包部署时清单先行注册，启动期暴露配置与代码不一致）
     */
    private void checkDeclaredControllers(PluginManifest manifest) {
        List<String> controllers = manifest.getControllers();
        if (controllers == null || controllers.isEmpty()) {
            return;
        }
        ClassLoader classLoader = resolveClassLoader();
        for (String controllerName : controllers) {
            if (controllerName == null || controllerName.isBlank()) {
                continue;
            }
            try {
                Class.forName(controllerName, false, classLoader);
            } catch (ClassNotFoundException e) {
                log.warn("插件清单声明的 controller 类不存在: {} (插件: {})",
                        controllerName, manifest.getPluginId());
            }
        }
    }

    private ClassLoader resolveClassLoader() {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader != null ? contextLoader : getClass().getClassLoader();
    }
}

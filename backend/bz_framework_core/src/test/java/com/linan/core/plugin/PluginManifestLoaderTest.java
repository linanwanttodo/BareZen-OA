package com.linan.core.plugin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.linan.core.plugin.controller.PluginController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PluginManifestLoader 单元测试
 *
 * <p>覆盖：真实 classpath 清单（plugins/salary-plugin.json）解析注册、
 * 启动自检对缺失 controller 类打 WARN、坏 JSON 清单不阻断其他清单注册。</p>
 */
class PluginManifestLoaderTest {

    private final PluginGateState gateState = new PluginGateState();
    private final PluginManager pluginManager = new PluginManager(gateState);

    @Test
    @DisplayName("解析真实 classpath 清单并注册：字段完整、门控登记为 LOADED")
    void parsesRealManifestAndRegisters() {
        PluginManifestLoader loader = new PluginManifestLoader(pluginManager, gateState);
        loader.loadManifests();

        PluginManifest manifest = pluginManager.getManifest("com.linan.plugin.salary");
        assertNotNull(manifest);
        assertEquals("工资管理模块", manifest.getPluginName());
        assertEquals("1.0.0", manifest.getVersion());
        assertNotNull(manifest.getControllers());
        assertEquals(2, manifest.getControllers().size());
        assertTrue(manifest.getControllers()
                .contains("com.linan.plugin.salary.controller.SalaryStructureController"));
        assertNotNull(manifest.getPermissions());
        assertTrue(manifest.getPermissions().contains("salary:structure:list"));

        assertEquals(PluginStatus.LOADED, gateState.getStatus("com.linan.plugin.salary"));
        assertFalse(gateState.isActive("com.linan.plugin.salary"));
    }

    @Test
    @DisplayName("frontendRoutes 清单为对象数组，解析为 Map 列表而非 String")
    void parsesFrontendRoutesAsMaps() {
        PluginManifestLoader loader = new PluginManifestLoader(pluginManager, gateState);
        loader.loadManifests();

        PluginManifest manifest = pluginManager.getManifest("com.linan.plugin.salary");
        assertNotNull(manifest);
        assertNotNull(manifest.getFrontendRoutes());
        assertEquals(1, manifest.getFrontendRoutes().size());
        assertEquals("/salary", manifest.getFrontendRoutes().get(0).get("path"));
    }

    @Test
    @DisplayName("启动自检：清单声明的 controller 类不存在时打 WARN，存在的类不打")
    void warnsWhenDeclaredControllerClassMissing() {
        Logger logger = (Logger) LoggerFactory.getLogger(PluginManifestLoader.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            PluginManifestLoader loader = new PluginManifestLoader(pluginManager, gateState,
                    "classpath:plugin-selfcheck/selfcheck-plugin.json");
            loader.loadManifests();
        } finally {
            logger.detachAppender(appender);
        }

        List<ILoggingEvent> warns = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();
        assertEquals(1, warns.size());
        String message = warns.get(0).getFormattedMessage();
        assertTrue(message.contains("com.linan.plugin.notexist.MissingController"));
        assertTrue(message.contains("com.linan.plugin.selfcheck"));
    }

    @Test
    @DisplayName("启动自检：命名空间外的声明拒绝自检并打 WARN，命名空间内缺失仍报类不存在")
    void rejectsOutOfNamespaceDeclaredControllers() {
        Logger logger = (Logger) LoggerFactory.getLogger(PluginManifestLoader.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            PluginManifestLoader loader = new PluginManifestLoader(pluginManager, gateState,
                    "classpath:plugin-namespace/outsider-plugin.json");
            loader.loadManifests();
        } finally {
            logger.detachAppender(appender);
        }

        List<String> warns = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(event -> event.getFormattedMessage())
                .toList();
        assertEquals(2, warns.size());
        assertTrue(warns.get(0).contains("java.lang.Runtime"));
        assertTrue(warns.get(0).contains("不在允许命名空间内"));
        assertTrue(warns.get(1).contains("com.linan.plugin.notexist.OutsideMissing"));
        assertTrue(warns.get(1).contains("类不存在"));
    }

    @Test
    @DisplayName("坏 JSON 清单不阻断其他清单：正常清单仍注册，坏清单未注册")
    void brokenJsonDoesNotBlockOtherManifests() {
        PluginManifestLoader loader = new PluginManifestLoader(pluginManager, gateState,
                "classpath:plugin-selfcheck/*.json");
        loader.loadManifests();

        assertNotNull(pluginManager.getManifest("com.linan.plugin.selfcheck"));
        assertTrue(gateState.getKnownPluginIds().contains("com.linan.plugin.selfcheck"));
        assertEquals(PluginStatus.LOADED, gateState.getStatus("com.linan.plugin.selfcheck"));
    }

    @Test
    @DisplayName("清单注册后详情接口可读到清单内容（与 PluginController 契约对齐）")
    void registeredManifestVisibleToDetailEndpoint() {
        PluginManifestLoader loader = new PluginManifestLoader(pluginManager, gateState);
        loader.loadManifests();

        PluginController controller = new PluginController(pluginManager, gateState);
        PluginController.PluginDetail detail = controller.getPlugin("com.linan.plugin.salary").getData();

        assertNotNull(detail);
        assertEquals("com.linan.plugin.salary", detail.getPluginId());
        assertFalse(detail.isLoaded());
        assertEquals(PluginStatus.LOADED.name(), detail.getStatus());
    }
}

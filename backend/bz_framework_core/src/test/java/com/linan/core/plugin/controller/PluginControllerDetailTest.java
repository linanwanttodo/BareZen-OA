package com.linan.core.plugin.controller;

import com.linan.core.common.api.Result;
import com.linan.core.plugin.PluginException;
import com.linan.core.plugin.PluginGateState;
import com.linan.core.plugin.PluginManager;
import com.linan.core.plugin.PluginManifest;
import com.linan.core.plugin.PluginStatus;
import com.linan.core.plugin.StubLifecyclePlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PluginController 详情接口单元测试
 *
 * <p>覆盖验收语义：GET /api/system/v1/plugins/{id} 返回清单内容 + 运行状态，
 * 并覆盖 启动/停止/卸载/仅清单未加载 各状态下的返回口径。</p>
 */
class PluginControllerDetailTest {

    private static final String PLUGIN_ID = "com.linan.plugin.salary";

    private final PluginGateState gateState = new PluginGateState();
    private final PluginManager pluginManager = new PluginManager(gateState);
    private final PluginController controller = new PluginController(pluginManager, gateState);

    private PluginManifest buildManifest() {
        PluginManifest manifest = new PluginManifest();
        manifest.setPluginId(PLUGIN_ID);
        manifest.setPluginName("工资管理模块");
        manifest.setVersion("1.0.0");
        manifest.setDescription("工资插件");
        manifest.setAuthor("BareZen Team");
        manifest.setDependencies(List.of());
        manifest.setControllers(List.of(
                "com.linan.plugin.salary.controller.SalaryStructureController",
                "com.linan.plugin.salary.controller.SalaryPaymentController"));
        manifest.setPermissions(List.of("salary:structure:list"));
        return manifest;
    }

    @Test
    @DisplayName("插件已启动：返回清单内容 + 运行状态 ACTIVE + loaded=true")
    void startedPlugin_returnsManifestAndActiveStatus() throws PluginException {
        pluginManager.registerManifest(buildManifest());
        pluginManager.loadPlugin(new StubLifecyclePlugin(PLUGIN_ID));
        pluginManager.startPlugin(PLUGIN_ID);

        Result<PluginController.PluginDetail> result = controller.getPlugin(PLUGIN_ID);

        assertEquals(200, result.getCode());
        PluginController.PluginDetail detail = result.getData();
        assertEquals(PLUGIN_ID, detail.getPluginId());
        assertEquals("工资管理模块", detail.getPluginName());
        assertEquals("1.0.0", detail.getVersion());
        assertEquals(2, detail.getControllers().size());
        assertTrue(detail.getPermissions().contains("salary:structure:list"));
        assertEquals(PluginStatus.ACTIVE.name(), detail.getStatus());
        assertTrue(detail.isLoaded());
        assertTrue(detail.isAvailable());
        assertFalse(detail.isEnabled());
    }

    @Test
    @DisplayName("插件已停止：运行状态回显 DISABLED")
    void stoppedPlugin_returnsDisabled() throws PluginException {
        pluginManager.registerManifest(buildManifest());
        pluginManager.loadPlugin(new StubLifecyclePlugin(PLUGIN_ID));
        pluginManager.startPlugin(PLUGIN_ID);
        pluginManager.stopPlugin(PLUGIN_ID);

        PluginController.PluginDetail detail = controller.getPlugin(PLUGIN_ID).getData();

        assertEquals(PluginStatus.DISABLED.name(), detail.getStatus());
        assertTrue(detail.isLoaded());
        assertFalse(detail.isAvailable());
    }

    @Test
    @DisplayName("插件已卸载：清单与实例均移除，返回插件不存在")
    void unloadedPlugin_returnsNotFound() throws PluginException {
        pluginManager.registerManifest(buildManifest());
        pluginManager.loadPlugin(new StubLifecyclePlugin(PLUGIN_ID));
        pluginManager.startPlugin(PLUGIN_ID);
        pluginManager.unloadPlugin(PLUGIN_ID);

        Result<PluginController.PluginDetail> result = controller.getPlugin(PLUGIN_ID);

        assertEquals(500, result.getCode());
        assertEquals("插件不存在", result.getMsg());
        assertNull(result.getData());
        assertFalse(gateState.isActive(PLUGIN_ID));
    }

    @Test
    @DisplayName("仅注册清单未加载插件：loaded=false，状态取门控状态")
    void manifestOnly_returnsGateStatus() {
        pluginManager.registerManifest(buildManifest());

        PluginController.PluginDetail detail = controller.getPlugin(PLUGIN_ID).getData();

        assertFalse(detail.isLoaded());
        assertEquals(PluginStatus.LOADED.name(), detail.getStatus());
        assertEquals("工资管理模块", detail.getPluginName());
    }

    @Test
    @DisplayName("未注册清单且未加载：返回插件不存在")
    void unknownPlugin_returnsNotFound() {
        Result<PluginController.PluginDetail> result = controller.getPlugin("com.linan.plugin.absent");

        assertEquals("插件不存在", result.getMsg());
        assertNull(result.getData());
    }
}

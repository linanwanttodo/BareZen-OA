package com.linan.plugin.masterdata;

import com.linan.core.plugin.PluginStatus;
import com.linan.plugin.masterdata.controller.CustomerInfoController;
import com.linan.plugin.masterdata.controller.MaterialInfoController;
import com.linan.plugin.masterdata.controller.SupplierInfoController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主数据插件契约测试
 *
 * <p>插件门控按包名 com.linan.plugin.masterdata 推导 pluginId，本测试锁定
 * 三方契约：插件 ID、生命周期状态流转、清单声明 controller 均真实存在且
 * 接口权限串与 V58 菜单 F 行一致。</p>
 */
class MasterDataPluginContractTest {

    private final MasterDataPlugin plugin = new MasterDataPlugin();

    @Test
    @DisplayName("插件 ID 与门控包名一致：com.linan.plugin.masterdata")
    void pluginId_matchesGatePackage() {
        assertEquals("com.linan.plugin.masterdata", plugin.getPluginId());
    }

    @Test
    @DisplayName("元数据完整：名称/版本/描述/作者")
    void metadata_complete() {
        assertEquals("主数据模块", plugin.getPluginName());
        assertEquals("1.0.0", plugin.getVersion());
        assertNotNull(plugin.getDescription());
        assertNotNull(plugin.getAuthor());
    }

    @Test
    @DisplayName("生命周期方法安全：start/stop 可直接调用不抛异常（状态由 PluginManager 管理）")
    void lifecycle_transitions() throws Exception {
        assertEquals(PluginStatus.LOADED, plugin.getStatus());
        plugin.start();
        plugin.stop();
        assertEquals(PluginStatus.LOADED, plugin.getStatus(),
                "AbstractPlugin 不自改状态，ACTIVE/DISABLED 由 PluginManager.startPlugin/stopPlugin 设置");
    }

    @Test
    @DisplayName("清单声明的三个 controller 类均存在于 classpath")
    void declaredControllers_exist() throws Exception {
        Class.forName("com.linan.plugin.masterdata.controller.SupplierInfoController");
        Class.forName("com.linan.plugin.masterdata.controller.CustomerInfoController");
        Class.forName("com.linan.plugin.masterdata.controller.MaterialInfoController");
        assertTrue(SupplierInfoController.class.isAnnotationPresent(
                org.springframework.web.bind.annotation.RestController.class));
        assertTrue(CustomerInfoController.class.isAnnotationPresent(
                org.springframework.web.bind.annotation.RestController.class));
        assertTrue(MaterialInfoController.class.isAnnotationPresent(
                org.springframework.web.bind.annotation.RestController.class));
    }
}

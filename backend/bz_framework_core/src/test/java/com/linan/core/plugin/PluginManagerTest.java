package com.linan.core.plugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PluginManager 状态流转单元测试
 *
 * <p>覆盖：load/start/stop/unload 状态流转、门控状态同步、
 * 重复加载拒绝、未知插件操作拒绝、生命周期回调异常时的 FAILED/ERROR 落态。</p>
 */
class PluginManagerTest {

    private final PluginGateState gateState = new PluginGateState();
    private final PluginManager pluginManager = new PluginManager(gateState);

    /** start/stop 调用计数可配置异常的插件桩 */
    private static class CountingPlugin extends StubLifecyclePlugin {
        private final AtomicInteger startCalls = new AtomicInteger();
        private final AtomicInteger stopCalls = new AtomicInteger();
        private final boolean failInitialize;
        private final boolean failStart;
        private final boolean failStop;

        CountingPlugin(String pluginId) {
            this(pluginId, false, false, false);
        }

        CountingPlugin(String pluginId, boolean failInitialize, boolean failStart, boolean failStop) {
            super(pluginId);
            this.failInitialize = failInitialize;
            this.failStart = failStart;
            this.failStop = failStop;
        }

        @Override
        public void initialize() throws PluginException {
            if (failInitialize) {
                throw new PluginException(getPluginId(), "初始化失败");
            }
        }

        @Override
        public void start() throws PluginException {
            if (failStart) {
                throw new PluginException(getPluginId(), "启动失败");
            }
            startCalls.incrementAndGet();
        }

        @Override
        public void stop() throws PluginException {
            if (failStop) {
                throw new PluginException(getPluginId(), "停止失败");
            }
            stopCalls.incrementAndGet();
        }
    }

    @Test
    @DisplayName("加载后 LOADED（门控关闭），启动后 ACTIVE（门控放行）")
    void loadThenStart_active() throws PluginException {
        CountingPlugin plugin = new CountingPlugin("com.linan.plugin.test");

        pluginManager.loadPlugin(plugin);
        assertEquals(PluginStatus.LOADED, plugin.getStatus());
        assertFalse(gateState.isActive("com.linan.plugin.test"));

        pluginManager.startPlugin("com.linan.plugin.test");
        assertEquals(PluginStatus.ACTIVE, plugin.getStatus());
        assertTrue(gateState.isActive("com.linan.plugin.test"));
        assertEquals(1, plugin.startCalls.get());
    }

    @Test
    @DisplayName("停止后 DISABLED（门控关闭），可再次启动回到 ACTIVE")
    void stopThenRestart() throws PluginException {
        pluginManager.loadPlugin(new CountingPlugin("com.linan.plugin.test"));
        pluginManager.startPlugin("com.linan.plugin.test");

        pluginManager.stopPlugin("com.linan.plugin.test");
        assertEquals(PluginStatus.DISABLED,
                pluginManager.getPlugin("com.linan.plugin.test").getStatus());
        assertFalse(gateState.isActive("com.linan.plugin.test"));

        pluginManager.startPlugin("com.linan.plugin.test");
        assertTrue(gateState.isActive("com.linan.plugin.test"));
    }

    @Test
    @DisplayName("卸载后插件从管理器移除，门控条目保留且关闭（拒绝服务）")
    void unload_keepsGateClosed() throws PluginException {
        CountingPlugin plugin = new CountingPlugin("com.linan.plugin.test");
        pluginManager.loadPlugin(plugin);
        pluginManager.startPlugin("com.linan.plugin.test");

        pluginManager.unloadPlugin("com.linan.plugin.test");

        assertNull(pluginManager.getPlugin("com.linan.plugin.test"));
        assertFalse(gateState.isActive("com.linan.plugin.test"));
        assertTrue(gateState.getKnownPluginIds().contains("com.linan.plugin.test"));
        assertEquals(PluginStatus.DISABLED, gateState.getStatus("com.linan.plugin.test"));
        assertEquals(1, plugin.stopCalls.get());
    }

    @Test
    @DisplayName("LOADED 状态直接卸载：不触发 stop，门控保持关闭")
    void unloadWithoutStart() throws PluginException {
        CountingPlugin plugin = new CountingPlugin("com.linan.plugin.test");
        pluginManager.loadPlugin(plugin);

        pluginManager.unloadPlugin("com.linan.plugin.test");

        assertNull(pluginManager.getPlugin("com.linan.plugin.test"));
        assertEquals(PluginStatus.DISABLED, gateState.getStatus("com.linan.plugin.test"));
        assertEquals(0, plugin.stopCalls.get());
    }

    @Test
    @DisplayName("重复加载同一插件被拒绝")
    void duplicateLoad_rejected() throws PluginException {
        pluginManager.loadPlugin(new CountingPlugin("com.linan.plugin.test"));

        assertThrows(PluginException.class,
                () -> pluginManager.loadPlugin(new CountingPlugin("com.linan.plugin.test")));
    }

    @Test
    @DisplayName("对不存在插件的 start/stop/unload 抛 PluginException")
    void unknownPlugin_rejected() {
        assertThrows(PluginException.class,
                () -> pluginManager.startPlugin("com.linan.plugin.absent"));
        assertThrows(PluginException.class,
                () -> pluginManager.stopPlugin("com.linan.plugin.absent"));
        assertThrows(PluginException.class,
                () -> pluginManager.unloadPlugin("com.linan.plugin.absent"));
        assertFalse(gateState.isActive("com.linan.plugin.absent"));
    }

    @Test
    @DisplayName("初始化异常：插件落 FAILED，门控关闭，插件不进入管理器")
    void initializeFailure_failed() {
        assertThrows(PluginException.class,
                () -> pluginManager.loadPlugin(new CountingPlugin("com.linan.plugin.bad", true, false, false)));

        assertNull(pluginManager.getPlugin("com.linan.plugin.bad"));
        assertEquals(PluginStatus.FAILED, gateState.getStatus("com.linan.plugin.bad"));
    }

    @Test
    @DisplayName("启动异常：插件落 FAILED，门控关闭")
    void startFailure_failed() throws PluginException {
        pluginManager.loadPlugin(new CountingPlugin("com.linan.plugin.bad", false, true, false));

        assertThrows(PluginException.class,
                () -> pluginManager.startPlugin("com.linan.plugin.bad"));

        assertEquals(PluginStatus.FAILED, gateState.getStatus("com.linan.plugin.bad"));
        assertFalse(gateState.isActive("com.linan.plugin.bad"));
    }

    @Test
    @DisplayName("停止异常：插件落 ERROR，门控关闭")
    void stopFailure_error() throws PluginException {
        pluginManager.loadPlugin(new CountingPlugin("com.linan.plugin.bad", false, false, true));
        pluginManager.startPlugin("com.linan.plugin.bad");

        assertThrows(PluginException.class,
                () -> pluginManager.stopPlugin("com.linan.plugin.bad"));

        assertEquals(PluginStatus.ERROR, gateState.getStatus("com.linan.plugin.bad"));
        assertFalse(gateState.isActive("com.linan.plugin.bad"));
    }
}

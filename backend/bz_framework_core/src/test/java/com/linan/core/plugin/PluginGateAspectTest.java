package com.linan.core.plugin;

import com.linan.core.common.api.Result;
import com.linan.plugin.gatetest.StubGateController;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PluginGateAspect 门控切面单元测试
 *
 * <p>覆盖验收语义：插件非 ACTIVE（停用/未启动/卸载/未注册）时，
 * com.linan.plugin 包下 Controller 的接口被拒绝（HTTP 503 + 统一 Result code=503），
 * 业务方法不触达；ACTIVE 时正常放行。</p>
 */
class PluginGateAspectTest {

    private static final String STUB_PLUGIN_ID = "com.linan.plugin.gatetest";

    private PluginGateState gateState;
    private PluginGateAspect aspect;
    private StubGateController target;
    private StubGateController proxy;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        gateState = new PluginGateState();
        aspect = new PluginGateAspect(gateState);
        target = new StubGateController();

        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        proxy = factory.getProxy();

        response = Mockito.mock(HttpServletResponse.class);
        ServletRequestAttributes attrs = Mockito.mock(ServletRequestAttributes.class);
        Mockito.when(attrs.getResponse()).thenReturn(response);
        RequestContextHolder.setRequestAttributes(attrs);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("插件 ACTIVE 时放行，返回业务结果")
    void activePlugin_proceeds() {
        gateState.update(STUB_PLUGIN_ID, PluginStatus.ACTIVE);

        Result<String> result = proxy.list();
        assertEquals(200, result.getCode());
        assertEquals("ok", result.getData());
        assertEquals(1, target.getCallCount());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("插件停用（DISABLED）时拒绝服务：HTTP 503、统一错误、业务方法不触达")
    void disabledPlugin_refuses() {
        gateState.update(STUB_PLUGIN_ID, PluginStatus.DISABLED);

        Result<?> result = proxy.list();

        assertEquals(503, result.getCode());
        assertEquals("插件已停用", result.getMsg());
        assertEquals(0, target.getCallCount());
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("插件卸载后仍拒绝服务（PluginManager.unload 保留门控条目并保持关闭）")
    void unloadedPlugin_refuses() throws PluginException {
        PluginManager pluginManager = new PluginManager(gateState);
        pluginManager.loadPlugin(new StubLifecyclePlugin(STUB_PLUGIN_ID));
        pluginManager.startPlugin(STUB_PLUGIN_ID);
        Result<String> activeResult = proxy.list();
        assertEquals(200, activeResult.getCode());
        assertEquals("ok", activeResult.getData());
        assertEquals(1, target.getCallCount());

        pluginManager.unloadPlugin(STUB_PLUGIN_ID);

        Result<?> result = proxy.list();

        assertEquals(503, result.getCode());
        assertEquals("插件已停用", result.getMsg());
        assertEquals(1, target.getCallCount());
    }

    @Test
    @DisplayName("插件未启动（LOADED）时拒绝服务，提示插件未启动")
    void loadedButNotStartedPlugin_refuses() {
        gateState.update(STUB_PLUGIN_ID, PluginStatus.LOADED);

        Result<?> result = proxy.list();

        assertEquals(503, result.getCode());
        assertEquals("插件未启动", result.getMsg());
        assertEquals(0, target.getCallCount());
    }

    @Test
    @DisplayName("包下未注册门控的插件（无门控条目）默认拒绝服务")
    void unknownPlugin_refuses() {
        Result<?> result = proxy.list();

        assertEquals(503, result.getCode());
        assertEquals("插件未注册或未启动", result.getMsg());
        assertEquals(0, target.getCallCount());
    }

    @Test
    @DisplayName("插件启动失败（FAILED）时拒绝服务")
    void failedPlugin_refuses() {
        gateState.update(STUB_PLUGIN_ID, PluginStatus.FAILED);

        Result<?> result = proxy.list();

        assertEquals("插件启动失败，已停止服务", result.getMsg());
        assertEquals(0, target.getCallCount());
    }

    @Test
    @DisplayName("非 HTTP 调用（无 RequestContext）时不抛异常，仍返回统一错误")
    void noRequestContext_refusesWithoutException() {
        gateState.update(STUB_PLUGIN_ID, PluginStatus.DISABLED);
        RequestContextHolder.resetRequestAttributes();

        Result<?> result = proxy.list();

        assertEquals(503, result.getCode());
        assertTrue(result.getMsg().contains("插件"));
        assertEquals(0, target.getCallCount());
    }
}

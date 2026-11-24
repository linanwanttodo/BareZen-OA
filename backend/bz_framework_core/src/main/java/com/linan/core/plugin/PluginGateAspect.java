package com.linan.core.plugin;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 插件门控切面
 *
 * <p>按包约定匹配插件 Controller：类位于 com.linan.plugin.{module}. 包下且标注
 * @RestController 的方法执行前，检查对应插件（pluginId = com.linan.plugin.{module}）
 * 在 {@link PluginGateState} 中是否为 ACTIVE；非 ACTIVE（含未注册、已卸载）时
 * 拒绝服务，返回统一错误（HTTP 503 / Result code=503）。</p>
 *
 * <p>状态由 {@link PluginManager} 在 start/stop/unload 时同步更新，
 * 因此管理端停止或卸载插件后，其业务接口立即拒绝服务。</p>
 *
 * @author BareZen Team
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PluginGateAspect {

    static final String PLUGIN_PACKAGE_PREFIX = "com.linan.plugin.";

    private final PluginGateState pluginGateState;

    @Pointcut("within(com.linan.plugin..*)")
    public void inPluginPackage() {}

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restControllerClass() {}

    @Around("inPluginPackage() && restControllerClass()")
    public Object gate(ProceedingJoinPoint joinPoint) throws Throwable {
        String pluginId = resolvePluginId(joinPoint);
        if (pluginId == null || pluginGateState.isActive(pluginId)) {
            return joinPoint.proceed();
        }

        setHttpResponseStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        PluginStatus status = pluginGateState.getStatus(pluginId);
        return new Result<>(ResultCodeEnum.PLUGIN_DISABLED.getCode(), refusedMessage(status), null);
    }

    /**
     * 从连接点声明类的包名推导插件ID：com.linan.plugin.{module}.xxx -> com.linan.plugin.{module}
     *
     * @return 插件ID；类不在插件包内或缺少模块段时返回 null（不门控）
     */
    static String resolvePluginId(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringType().getName();
        if (!className.startsWith(PLUGIN_PACKAGE_PREFIX)) {
            return null;
        }
        String rest = className.substring(PLUGIN_PACKAGE_PREFIX.length());
        int firstDot = rest.indexOf('.');
        if (firstDot <= 0) {
            return null;
        }
        return PLUGIN_PACKAGE_PREFIX + rest.substring(0, firstDot);
    }

    private static String refusedMessage(PluginStatus status) {
        if (status == null) {
            return "插件未注册或未启动";
        }
        return switch (status) {
            case LOADED -> "插件未启动";
            case ENABLED -> "插件未启动";
            case DISABLED -> "插件已停用";
            case FAILED -> "插件启动失败，已停止服务";
            case ERROR -> "插件异常，已停止服务";
            case ACTIVE -> "插件已停用";
        };
    }

    private void setHttpResponseStatus(int httpStatus) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs.getResponse() != null) {
            attrs.getResponse().setStatus(httpStatus);
        }
    }
}

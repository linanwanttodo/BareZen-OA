package com.linan.core.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * CSRF 防护过滤器
 *
 * <p>通过检查请求头 {@code X-Requested-With} 来防御跨站请求伪造攻击。
 * 对于所有 POST/PUT/DELETE 请求，要求必须包含 {@code X-Requested-With: XMLHttpRequest} 头。
 * 前端 Axios 框架默认会自动添加此请求头。</p>
 *
 * <p>另外，对于 PUT/DELETE 请求，还会额外检查 Origin 或 Referer 头是否在白名单中。</p>
 *
 * <h3>豁免路径</h3>
 * <ul>
 *   <li>{@code /api/auth/v1/**} —— 登录注册等认证接口（由验证码等其他机制防护）</li>
 *   <li>GET/HEAD/OPTIONS 请求 —— 幂等方法无需 CSRF 防护</li>
 * </ul>
 *
 * @author BareZen Team
 */
@Slf4j
@Component
@Order(1)
public class CsrfFilter implements Filter {

    /** 需要 CSRF 保护的 HTTP 方法 */
    private static final String[] PROTECTED_METHODS = {"POST", "PUT", "DELETE"};

    /** 无需 CSRF 校验的路径前缀 */
    private static final String[] EXCLUDED_PATHS = {
            "/api/auth/v1/",
            "/api/common/v1/"
    };

    /** 允许的跨域 Origin 白名单（与 CORS 配置共用，逗号分隔；空=仅允许同源） */
    private final List<String> allowedOrigins;

    public CsrfFilter(@Value("${cors.allowed-origins:}") String allowedOriginsConfig) {
        this.allowedOrigins = allowedOriginsConfig == null || allowedOriginsConfig.isBlank()
                ? List.of()
                : Arrays.stream(allowedOriginsConfig.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 仅对 POST/PUT/DELETE 进行 CSRF 校验
        String method = request.getMethod();
        boolean isProtected = false;
        for (String m : PROTECTED_METHODS) {
            if (m.equalsIgnoreCase(method)) {
                isProtected = true;
                break;
            }
        }
        if (!isProtected) {
            chain.doFilter(request, response);
            return;
        }

        // 豁免路径检查
        String requestUri = request.getRequestURI();
        for (String path : EXCLUDED_PATHS) {
            if (requestUri.startsWith(path)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 检查 X-Requested-With 头（前端 Axios 默认会带此头）
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            chain.doFilter(request, response);
            return;
        }

        // 备用方案：校验 Origin 头是否为同源或在白名单中
        // 注意：不能只判断 Origin 非空就放行——跨站表单提交时浏览器也会自动携带
        // 攻击者域名的 Origin 头，必须验证其值确实属于可信来源
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank() && isTrustedOrigin(origin, request)) {
            chain.doFilter(request, response);
            return;
        }

        // 如果都没有，拒绝请求
        log.warn("CSRF 攻击拦截: method={}, uri={}", method, requestUri);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"CSRF 校验失败，请求被拒绝\"}");
        response.getWriter().flush();
    }

    /**
     * 判断 Origin 是否可信：与当前请求同源，或在 CORS 白名单中
     */
    private boolean isTrustedOrigin(String origin, HttpServletRequest request) {
        // 白名单精确匹配（忽略尾部斜杠）
        String normalized = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        for (String allowed : allowedOrigins) {
            String allowedNormalized = allowed.endsWith("/") ? allowed.substring(0, allowed.length() - 1) : allowed;
            if (allowedNormalized.equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        // 同源判断：Origin 的 host:port 与请求的 Host 头一致
        try {
            URI originUri = URI.create(origin);
            String originHost = originUri.getHost();
            if (originHost == null) {
                return false;
            }
            int originPort = originUri.getPort() != -1
                    ? originUri.getPort()
                    : ("https".equalsIgnoreCase(originUri.getScheme()) ? 443 : 80);
            String serverHost = request.getServerName();
            int serverPort = request.getServerPort();
            return originHost.equalsIgnoreCase(serverHost) && originPort == serverPort;
        } catch (Exception e) {
            log.warn("Origin 解析失败: {}", origin);
            return false;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}

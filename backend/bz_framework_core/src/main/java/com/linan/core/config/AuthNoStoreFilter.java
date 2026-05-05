package com.linan.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 认证与开放接口 no-store 响应头过滤器
 *
 * <p>登录、刷新 Token 等认证接口（{@code /api/auth/**}）与开放接口（{@code /api/open/**}）
 * 的响应可能携带 Token 等敏感数据。若被浏览器/中间代理缓存，登出后可能出现
 * 缓存回退、残留登录态页面等安全问题，因此对这两类路径统一输出
 * {@code Cache-Control: no-store} + {@code Pragma: no-cache}。</p>
 *
 * <p>注册方式见 {@link WebMvcConfig#authNoStoreFilter()}：FilterRegistrationBean
 * urlPatterns 限定 {@code /api/auth/*}、{@code /api/open/*}，order 取
 * HIGHEST_PRECEDENCE（先于其他安全头过滤器执行）。其余路径由
 * {@link #shouldNotFilter} 排除，不进入本过滤器。</p>
 */
public class AuthNoStoreFilter extends OncePerRequestFilter {

    /** 需要添加 no-store 头的路径前缀（与 FilterRegistrationBean urlPatterns 一致） */
    private static final String[] MATCH_PREFIXES = {"/api/auth", "/api/open"};

    /**
     * 非认证/开放路径直接放行，不设置响应头
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String prefix : MATCH_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 认证/开放接口响应禁止缓存：避免登出后缓存回退或登录态残留
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        filterChain.doFilter(request, response);
    }
}
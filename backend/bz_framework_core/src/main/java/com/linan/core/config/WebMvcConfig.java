package com.linan.core.config;

import com.linan.core.security.JwtUtil;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.core.security.interceptor.OpenApiInterceptor;
import com.linan.system.service.ISysApiKeyService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * Spring MVC 全局配置类
 *
 * 负责注册拦截器、配置跨域、安全响应头等 Web 层基础设施。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RedisTemplate<String, Object> redisTemplate;

    /*
     * AuthInterceptor 实例
     */
    private final AuthInterceptor authInterceptor;

    /** 逗号分隔的允许来源，如 https://admin.example.com,https://app.example.com；空则开发模式放行 */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    /** JWT 过期时间（毫秒） */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * 通过构造函数注入 RedisTemplate、JwtUtil 和 JWT 过期时间
     */
    public WebMvcConfig(RedisTemplate<String, Object> redisTemplate, JwtUtil jwtUtil,
                        ISysApiKeyService apiKeyService,
                        @Value("${jwt.expiration:86400000}") long jwtExpiration) {
        this.redisTemplate = redisTemplate;
        this.authInterceptor = new AuthInterceptor(redisTemplate, jwtUtil, jwtExpiration);
        this.openApiInterceptor = new OpenApiInterceptor(apiKeyService);
    }

    /**
     * Open API 拦截器（/api/open/** 路径使用 API Key 认证，与 JWT 拦截器互斥）
     */
    private final OpenApiInterceptor openApiInterceptor;

    /**
     * 注册拦截器
     *
     * 将 AuthInterceptor 注册到 Spring MVC 的拦截器链中。
     * 拦截器按注册顺序执行，preHandle 先注册先执行，postHandle 先注册后执行。
     *
     * 路径匹配说明：
     * - "/api/**"   ：匹配所有 /api/ 开头的路径（多级子路径），如 /api/system/v1/users
     * - "/api/*"    ：仅匹配 /api/ 下的一级路径，如 /api/login，不匹配 /api/system/v1/users
     * - "/*.html"   ：匹配根路径下的 HTML 文件
     *
     * 排除路径说明：
     * 被 exclude 的路径不会经过拦截器，直接放行。
     * 通常排除登录、注册等认证相关接口，因为它们本身就是用于获取 Token 的。
     *
     * 注意：@PassAuth 注解的检查是在拦截器内部的 preHandle 方法中实现的，
     * excludePathPatterns 是在拦截器外部（Spring 层面）排除，
     * 两者都能实现放行效果，但 excludePathPatterns 更高效（完全不进入拦截器）。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // JWT 拦截器：拦截所有 /api/ 路径（/api/open/** 除外，由 Open API 拦截器接管）
        registry.addInterceptor(authInterceptor)               // 注册拦截器实例
                .addPathPatterns("/api/**")                    // 拦截所有 /api/ 路径
                .excludePathPatterns(                          // 排除以下路径
                        "/api/open/**",                        // 开放平台接口走 API Key 认证
                        "/api/auth/v1/login",                  // 登录接口（获取 Token）
                        "/api/auth/v1/register",               // 注册接口
                        "/api/auth/v1/captcha",                // 验证码接口
                        "/api/common/v1/health"                // 健康检查接口
                        // 文件下载需登录；公开文件在 Controller 内校验 scope
                );
        // Open API 拦截器：仅拦截 /api/open/**，通过 X-Api-Key 认证外部系统
        registry.addInterceptor(openApiInterceptor)
                .addPathPatterns("/api/open/**");
    }

    /**
     * 配置跨域资源共享（CORS）
     *
     * 当前端应用运行在不同的域名/端口时（如 Vue 开发服务器 localhost:5173），
     * 浏览器会发送跨域请求。CORS 配置告诉浏览器哪些来源是被允许的。
     *
     * 安全要求：
     * - 开发环境：允许 localhost:5173（管理端）和 localhost:5174（用户端）
     * - 生产环境：通过 cors.allowed-origins 配置具体域名
     * - 禁止 allowCredentials(true) 与通配符 * 同时使用（安全风险）
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        var cors = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        if (StringUtils.hasText(allowedOrigins)) {
            // 生产环境：使用配置的具体域名
            String[] origins = allowedOrigins.split(",");
            for (int i = 0; i < origins.length; i++) {
                origins[i] = origins[i].trim();
            }
            cors.allowedOrigins(origins)
                .allowCredentials(true);
        } else {
            // 开发模式：放行 localhost/127.0.0.1 任意端口（vite 端口被占自动换端口时不会被 403 拦截）
            // 使用 allowedOriginPatterns 支持端口通配，与 allowCredentials(true) 兼容
            cors.allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowCredentials(true);
        }
    }

    /*
     * ==================== 扩展点 ====================
     *
     * 以下方法可根据项目需要添加，目前注释以保持简洁。
     */

    /**
     * 配置静态资源映射 —— 允许前端直接通过 URL 访问上传的文件（头像等）。
     *
     * 将虚拟路径 /profile/** 映射到文件系统 <user.dir>/uploads/ 目录。
     * 例如：访问 http://localhost:8080/profile/avatar/2025/01/01/xxx.png
     *       实际读取 <user.dir>/uploads/avatar/2025/01/01/xxx.png
     *
     * 这种方式适用于头像、公开图片等不需要鉴权的静态文件。
     * 若需权限控制的私有文件，请使用 SysFileController.download() 接口。
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/profile/**")
                .addResourceLocations("file:./uploads/");
    }

    /**
     * 安全响应头过滤器
     *
     * 为所有响应添加安全相关的 HTTP 头，防止点击劫持、MIME 嗅探等攻击。
     */
    @Bean
    public FilterRegistrationBean<Filter> securityHeadersFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response,
                                 FilterChain chain) throws IOException, ServletException {
                HttpServletResponse resp = (HttpServletResponse) response;
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("X-XSS-Protection", "1; mode=block");
                resp.setHeader("Cache-Control", "no-store");
                resp.setHeader("Pragma", "no-cache");
                resp.setHeader("Content-Security-Policy",
                        "default-src 'self'; "
                        + "script-src 'self'; "
                        + "style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data: blob:; "
                        + "font-src 'self' data:; "
                        + "connect-src 'self'; "
                        + "frame-src 'none'; "
                        + "object-src 'none'; "
                        + "base-uri 'self'; "
                        + "form-action 'self'");
                chain.doFilter(request, response);
            }

            @Override
            public void init(FilterConfig filterConfig) {}
            @Override
            public void destroy() {}
        });
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
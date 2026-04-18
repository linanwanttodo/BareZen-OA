package com.linan.core.config;

import com.linan.core.security.JwtUtil;
import com.linan.core.security.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 全局配置类
 *
 * 负责注册拦截器、配置跨域、消息转换器等 Web 层基础设施。
 * 实现 WebMvcConfigurer 接口，覆盖需要自定义的方法。
 *
 * 注意：不要在此类中添加 @EnableWebMvc 注解，
 * 它会导致 Spring Boot 的自动配置失效（如静态资源映射、JSON 序列化配置等）。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RedisTemplate<String, Object> redisTemplate;

    /*
     * AuthInterceptor 实例
     */
    private final AuthInterceptor authInterceptor;

    /**
     * 通过构造函数注入 RedisTemplate 和 JwtUtil
     */
    public WebMvcConfig(RedisTemplate<String, Object> redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.authInterceptor = new AuthInterceptor(redisTemplate, jwtUtil);
    }

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
        registry.addInterceptor(authInterceptor)               // 注册拦截器实例
                .addPathPatterns("/api/**")                    // 拦截所有 /api/ 路径
                .excludePathPatterns(                          // 排除以下路径
                        "/api/auth/v1/login",                  // 登录接口（获取 Token）
                        "/api/auth/v1/register",               // 注册接口（如果存在）
                        "/api/auth/v1/captcha",                // 验证码接口
                        "/api/common/v1/health",               // 健康检查接口
                        "/api/system/v1/files/download/**"     // 文件下载（公开链接）
                );
        // 如果需要排除多个模式，可以链式调用多次 excludePathPatterns，或一次传入多个 pattern
    }

    /**
     * 配置跨域资源共享（CORS）
     *
     * 当前端应用运行在不同的域名/端口时（如 Vue 开发服务器 localhost:5173），
     * 浏览器会发送跨域请求。CORS 配置告诉浏览器哪些来源是被允许的。
     *
     * 当前配置允许所有来源（开发阶段），生产环境应限制具体的域名。
     *
     * CORS 相关 HTTP 响应头说明：
     * - Access-Control-Allow-Origin：允许的请求来源
     * - Access-Control-Allow-Methods：允许的 HTTP 方法
     * - Access-Control-Allow-Headers：允许的请求头
     * - Access-Control-Allow-Credentials：是否允许携带 Cookie
     * - Access-Control-Max-Age：预检请求缓存时间（秒）
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")                         // 允许跨域的路径
                .allowedOriginPatterns("*")                    // 允许所有来源（开发环境）
                // 生产环境建议：
                // .allowedOrigins("https://your-admin-domain.com", "https://your-app-domain.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的 HTTP 方法
                .allowedHeaders("*")                           // 允许所有请求头
                .allowCredentials(true)                        // 允许携带 Cookie（如需要）
                .maxAge(3600);                                 // 预检请求缓存 1 小时
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
}
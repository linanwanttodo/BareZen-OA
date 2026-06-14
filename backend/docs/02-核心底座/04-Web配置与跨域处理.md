# 04-Web配置与跨域处理

## 1. 功能概述

`WebMvcConfig` 是 Spring MVC 的全局配置类，负责注册拦截器、配置跨域（CORS）、定制消息转换器等 Web 层基础设施。

在 Spring Boot 中，实现 `WebMvcConfigurer` 接口可以覆盖默认的 MVC 配置，而不影响 Spring Boot 的自动配置。这与使用 `@EnableWebMvc` 注解有本质区别——后者会导致 Spring Boot 自动配置完全失效（包括静态资源映射、JSON 序列化配置等），除非你有特殊需求，否则应避免使用 `@EnableWebMvc`。

**本章配置的主要功能**：
- 注册 AuthInterceptor 拦截器到 Spring MVC 拦截器链
- 配置 CORS 跨域规则，允许前端开发服务器访问接口
- 预留了 Jackson 序列化和静态资源映射的扩展点

---

## 2. 设计原理

### 2.1 WebMvcConfigurer 接口

Spring Boot 通过 `WebMvcAutoConfiguration` 自动配置 MVC 组件。当容器中存在 `WebMvcConfigurer` Bean 时，Spring Boot 会将其配置合并到自动配置中（而非替换）。这样既能享受 Spring Boot 自动配置的便利，又能自定义所需的部分。

### 2.2 拦截器执行链

```text
请求到达 -> Filter Chain -> DispatcherServlet -> 拦截器链 preHandle
                                                      |
                                  AuthInterceptor.preHandle (鉴权)
                                                      |
                                          /api/auth/** 被 exclude
                                          @PassAuth 标记 -> 放行
                                          携带有效 Token -> 放行
                                          其他 -> 返回 401
                                                      |
                                                    Controller
                                                      |
                                               拦截器链 postHandle
                                                      |
                                                  响应输出
```

### 2.3 CORS 跨域原理

当前端应用和后端 API 运行在不同的域名或端口时，浏览器的同源策略会阻止跨域请求。例如：
- 前端：`http://localhost:5173`（Vite 开发服务器）
- 后端：`http://localhost:8080`

浏览器会发送预检请求（OPTIONS 方法）询问后端是否允许跨域。后端通过在响应头中添加 CORS 头部来告知浏览器允许跨域。

---

## 3. 文件清单

| 文件路径 | 说明 |
| :--- | :--- |
| `bz_framework_core/src/main/java/com/linan/core/config/WebMvcConfig.java` | Spring MVC 全局配置类 |

---

## 4. 代码实现

文件路径：`bz_framework_core/src/main/java/com/linan/core/config/WebMvcConfig.java`

```java
package com.linan.core.config;

import com.linan.core.security.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
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

    /**
     * AuthInterceptor 实例
     * 在此创建单例，避免每次拦截时重复创建
     */
    private final AuthInterceptor authInterceptor = new AuthInterceptor();

    /**
     * 注册拦截器
     *
     * 将 AuthInterceptor 注册到 Spring MVC 的拦截器链中。
     * 拦截器按注册顺序执行，preHandle 先注册先执行，postHandle 先注册后执行。
     *
     * 路径匹配说明：
     * - "/api/**"   ：匹配所有 /api/ 开头的路径（多级子路径）
     * - "/api/*"    ：仅匹配 /api/ 下的一级路径
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)               // 注册拦截器实例
                .addPathPatterns("/api/**")                    // 拦截所有 /api/ 路径
                .excludePathPatterns(                          // 排除以下路径
                        "/api/auth/v1/login",                  // 登录接口（获取 Token）
                        "/api/auth/v1/register",               // 注册接口（如果存在）
                        "/api/auth/v1/captcha",                // 验证码接口
                        "/api/common/v1/health"                // 健康检查接口
                );
    }

    /**
     * 配置跨域资源共享（CORS）
     *
     * 当前端应用运行在不同的域名/端口时，浏览器会阻止跨域请求。
     * CORS 配置告诉浏览器哪些来源是被允许的。
     *
     * 当前配置允许所有来源（开发阶段），生产环境应限制具体域名。
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")                         // 允许跨域的路径
                .allowedOriginPatterns("*")                    // 允许所有来源（开发环境）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的方法
                .allowedHeaders("*")                           // 允许所有请求头
                .allowCredentials(true)                        // 允许携带 Cookie
                .maxAge(3600);                                 // 预检请求缓存 1 小时
    }
}
```

**逐行讲解**：

- `@Configuration`：Spring 注解，声明此类为配置类。Spring 容器启动时会扫描并处理此类中的 Bean 定义和方法覆盖。
- `implements WebMvcConfigurer`：实现此接口表示自定义 Spring MVC 配置。接口中所有方法都有默认实现，只需覆盖需要自定义的方法。
- `private final AuthInterceptor authInterceptor = new AuthInterceptor()`：在配置类中创建拦截器单例。由于 AuthInterceptor 是无状态的（没有成员变量），单例是安全的。如果拦截器有状态（如注入 Service），应使用 `@Autowired` 注入。
- `registry.addInterceptor(authInterceptor)`：将拦截器注册到拦截器链。可以链式调用配置路径匹配规则。
- `addPathPatterns("/api/**")`：使用 Ant 风格的路径匹配模式。`**` 匹配多级子路径。
- `excludePathPatterns(...)`：排除不需要拦截的路径。这些路径不会进入拦截器，节省开销。
- `allowedOriginPatterns("*")`：允许所有来源。Spring 5.3+ 推荐使用 `allowedOriginPatterns` 替代 `allowedOrigins`，因为前者支持更灵活的通配符匹配。
- `allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")`：显式列出允许的 HTTP 方法。OPTIONS 是必须包含的，因为浏览器的预检请求使用 OPTIONS 方法。
- `allowCredentials(true)`：允许携带 Cookie。如果前端使用 `withCredentials: true` 发送 Cookie，此配置必须为 true。但注意，当此值为 true 时，`allowedOrigins` 不能使用 `*` 通配符（可用 `allowedOriginPatterns("*")` 替代）。
- `maxAge(3600)`：预检请求缓存时间（秒）。浏览器在此时间内不会再次发送预检请求，减少网络开销。

---

## 5. excludePathPatterns 与 @PassAuth 的区别

两者都能实现放行效果，但机制不同：

| 方式 | 执行层面 | 何时使用 |
| :--- | :--- | :--- |
| `excludePathPatterns` | Spring 层面，拦截器完全不进入 | 确定永远不需要鉴权的路径（如登录接口），因为获取 Token 本身不需要 Token |
| `@PassAuth` | 拦截器内部，进入 preHandle 后检查注解 | 需要灵活控制的接口，如某些环境放行、某些环境不放行 |

**建议策略**：
- 认证相关接口（登录、注册、验证码）→ 使用 `excludePathPatterns`（性能最优）
- 其他可选的公开接口（如首页公告列表）→ 使用 `@PassAuth`（灵活性最高）

---

## 6. 路径匹配规则说明

Spring 使用 Ant 风格的路径匹配：

| 模式 | 匹配示例 | 不匹配示例 |
| :--- | :--- | :--- |
| `/api/**` | `/api/system/v1/users`, `/api/auth/login` | `/apix/login` |
| `/api/*` | `/api/login`, `/api/users` | `/api/system/v1/users` |
| `/api/users/*/detail` | `/api/users/1/detail` | `/api/users/1/orders/detail` |
| `/api/users/**/detail` | `/api/users/1/detail`, `/api/users/a/b/detail` | `/api/users/detail` |
| `/*.html` | `/index.html` | `/pages/index.html` |

---

## 7. CORS 配置详解

### 7.1 开发环境 vs 生产环境

```java
// 开发环境：允许所有来源（方便本地调试）
registry.addMapping("/api/**")
        .allowedOriginPatterns("*")
        .allowedMethods("*")
        .allowedHeaders("*")
        .allowCredentials(true);

// 生产环境：严格限制域名（安全最佳实践）
registry.addMapping("/api/**")
        .allowedOrigins(
            "https://admin.your-domain.com",
            "https://app.your-domain.com"
        )
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .allowedHeaders("Content-Type", "Authorization")
        .allowCredentials(true)
        .maxAge(7200);
```

### 7.2 预检请求（Preflight Request）

对于非简单请求（如 Content-Type 为 application/json 或使用了自定义 Header），浏览器在发送实际请求前会先发送一个 OPTIONS 请求来询问服务器是否允许跨域。

**简单请求的条件**（三个条件必须全部满足）：
1. 方法是 GET、HEAD 或 POST
2. Content-Type 是 `application/x-www-form-urlencoded`、`multipart/form-data` 或 `text/plain`
3. 没有自定义 Header

BareZen_OA 的请求通常不是简单请求（Content-Type 为 application/json 且携带 Authorization Header），因此每次跨域调用都会先发送 OPTIONS 预检请求。`maxAge(3600)` 告诉浏览器在 1 小时内缓存预检结果，避免重复请求。

---

## 8. 扩展：Jackson 序列化配置

如果需要自定义 JSON 序列化行为（如日期格式、null 值处理），可以在 `WebMvcConfig` 中覆盖 `configureMessageConverters` 方法：

```java
@Override
public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    ObjectMapper objectMapper = new ObjectMapper();

    // 日期格式
    objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    // 时区
    objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    // 序列化时忽略 null 值（可选，使响应更简洁）
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    converter.setObjectMapper(objectMapper);
    converters.add(0, converter);  // 添加到首位，优先级最高
}
```

或者在 `application.yml` 中配置（推荐，更简洁）：

```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
```

---

## 9. 扩展：静态资源映射

如果后端需要提供文件访问服务（如上传的头像），可配置虚拟路径映射：

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 将 URL 路径 /profile/** 映射到文件系统路径 ./upload/
    // 例如：访问 http://localhost:8080/profile/avatar/1.jpg
    //       实际读取 ./upload/avatar/1.jpg 文件
    registry.addResourceHandler("/profile/**")
            .addResourceLocations("file:./upload/");
}
```

---

## 10. 测试验证

### 10.1 验证拦截器注册

启动应用后，访问需要 Token 的接口：

```bash
curl http://localhost:8080/api/system/v1/users
# 预期响应：{"code":401,"msg":"请求头中缺少Authorization信息","data":null}
```

说明拦截器已正常拦截。

### 10.2 验证排除路径

```bash
# 登录接口被 exclude，不会返回 401
curl http://localhost:8080/api/auth/v1/login
# 预期响应（即使还没有实现登录逻辑，也不会是 401）
```

### 10.3 验证 CORS

在前端应用中（如 Vue 开发服务器）调用后端接口，浏览器网络面板中应看到 CORS 响应头：

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
Access-Control-Allow-Credentials: true
```

---

## 11. 常见问题

**Q: 添加了 CORS 配置但前端仍然报跨域错误？**

A: 检查以下几点：
1. 是否引入了 Spring Security（未正确配置的 Spring Security 会覆盖 CORS 设置）
2. `allowCredentials(true)` 和 `allowedOrigins("*")` 不能同时使用（Spring 会忽略），改用 `allowedOriginPatterns("*")`
3. 浏览器缓存了旧的预检结果，尝试清除缓存或使用无痕模式

**Q: 拦截器为什么不生效？**

A: 检查：
1. 拦截器是否在 `@Configuration` 类中注册
2. 路径模式是否匹配请求路径（区分 `/api/**` 和 `/api/*`）
3. 是否使用了 `@EnableWebMvc`（会导致此配置被 Spring Boot 自动配置覆盖）

**Q: 可以同时使用 excludePathPatterns 和 @PassAuth 吗？**

A: 可以。excludePathPatterns 在拦截器层面完全排除，不会进入 preHandle。@PassAuth 注解在拦截器内部起作用。使用 excludePathPatterns 排除的路径，即使没有 @PassAuth 也会放行。

---

## 12. 本章完成检查清单

- [ ] 理解 `WebMvcConfigurer` 接口和 `@EnableWebMvc` 的区别
- [ ] 能配置拦截器的路径匹配和排除规则
- [ ] 理解 `excludePathPatterns` 和 `@PassAuth` 各自的使用场景
- [ ] 理解 CORS 跨域原理和开发/生产环境的配置差异
- [ ] 理解 OPTIONS 预检请求的作用和缓存机制

---

*下一章：[05-SpringBoot主启动类编写](./05-SpringBoot主启动类编写.md)*
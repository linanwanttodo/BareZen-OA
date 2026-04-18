# 03-JWT鉴权引擎实现

## 1. 功能概述

BareZen_Base 采用轻量级的 JWT 拦截器方案实现身份认证，而不使用臃肿的 Spring Security。这是一个极简但完整的鉴权引擎，由三个核心组件组成。

**核心组件**：
- `JwtUtil`：Token 的生成与解析工具类
- `PassAuth`：自定义注解，标记公开接口（无需登录即可访问）
- `AuthInterceptor`：请求拦截器，校验 Header 中的 Token

**鉴权流程**：

```text
用户请求 -> AuthInterceptor 拦截
  |
  |-> 接口有 @PassAuth？ -> 是 -> 放行（无需 Token）
  |
  |-> 否 -> 提取 Authorization Header
         |-> 无 Token -> 返回 401 JSON
         |-> 有 Token -> JwtUtil.parseToken(token)
               |-> 解析成功 -> 放行，注入 userId 到 request
               |-> 解析失败 -> 返回 401 JSON
```

---

## 2. JWT 简介

JWT（JSON Web Token）是一种开放标准（RFC 7519），用于在各方之间安全地传输信息。一个 JWT Token 由三部分组成，用 `.` 分隔：

```text
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
|_________ 头部 ________|____ 载荷 ____|__ 签名 __|
```

- **Header（头部）**：声明签名算法（如 HMAC-SHA256）
- **Payload（载荷）**：存放用户信息（如 userId、username）
- **Signature（签名）**：用密钥对前两部分签名，防止篡改

**为什么选择 JWT 而非 Session？**
- 无状态：服务端不需要存储会话信息，水平扩展方便
- 跨域友好：Token 放在 Header 中，天然支持跨域
- 移动端友好：不依赖 Cookie

---

## 3. 文件清单

| 文件路径 | 说明 |
| :--- | :--- |
| `bz_framework_core/src/main/java/com/linan/core/security/JwtUtil.java` | JWT 工具类（生成与解析 Token） |
| `bz_framework_core/src/main/java/com/linan/core/security/PassAuth.java` | 跳过鉴权注解 |
| `bz_framework_core/src/main/java/com/linan/core/security/interceptor/AuthInterceptor.java` | JWT 鉴权拦截器 |

---

## 4. 代码实现

### 4.1 PassAuth.java — 跳过鉴权注解

文件路径：`bz_framework_core/src/main/java/com/linan/core/security/PassAuth.java`

```java
package com.linan.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过鉴权注解
 *
 * 标记在 Controller 方法或类上，表示该接口不需要登录即可访问。
 * 典型的用法包括：
 * - 登录接口（POST /api/auth/v1/login）
 * - 注册接口（POST /api/auth/v1/register）
 * - 健康检查接口（GET /api/common/v1/health）
 * - 公共数据接口（如首页公告列表）
 *
 * 使用方式：
 *
 * // 方法级别：仅当前方法放行
 * @PassAuth
 * @PostMapping("/login")
 * public Result<String> login() { ... }
 *
 * // 类级别：整个 Controller 的所有方法都放行
 * @PassAuth
 * @RestController
 * @RequestMapping("/api/auth/v1")
 * public class AuthController { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})  // 可标记在方法和类上
@Retention(RetentionPolicy.RUNTIME)               // 运行时保留，以便反射读取
public @interface PassAuth {
    // 这是一个标记注解，不需要任何属性
    // AuthInterceptor 通过检查该注解是否存在来决定是否放行
}
```

**逐行讲解**：

- `@Target({ElementType.METHOD, ElementType.TYPE})`：指定注解可应用于方法（METHOD）和类/接口（TYPE）。这使得 @PassAuth 既可以用在单个方法上，也可以用在 Controller 类上（表示整个 Controller 的所有方法都放行）。
- `@Retention(RetentionPolicy.RUNTIME)`：指定注解在运行时保留。AuthInterceptor 通过反射读取方法/类上的注解，所以必须保留到运行时。编译时注解（SOURCE）或类加载时注解（CLASS）无法被反射读取。
- 接口体为空：是标记注解（Marker Annotation），不需要属性。AuthInterceptor 只检查注解是否存在（`getAnnotation(PassAuth.class) != null`）。

### 4.2 JwtUtil.java — JWT 工具类

文件路径：`bz_framework_core/src/main/java/com/linan/core/security/JwtUtil.java`

```java
package com.linan.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 *
 * 负责 Token 的生成（createToken）与解析（parseToken）。
 * 使用 HMAC-SHA256 算法对 Token 签名，确保数据完整性。
 *
 * 安全提醒：SECRET 密钥在生产环境必须使用环境变量或配置中心管理，
 *          绝对不能硬编码在代码中或提交到版本控制系统。
 */
public class JwtUtil {

    /**
     * JWT 签名密钥
     *
     * 用于对 Token 进行 HMAC-SHA256 签名。
     * 密钥长度至少需要 256 位（32 字节），HMAC-SHA256 才能正常生效。
     * 当前字符串 "BareZenBaseSecretKeyForJWTSigning2024" 长度为 37 字节，满足要求。
     *
     * 安全警告：生产环境必须通过环境变量读取（如 System.getenv("JWT_SECRET")），
     *         硬编码密钥存在严重安全风险。
     */
    private static final String SECRET = "BareZenBaseSecretKeyForJWTSigning2024";

    /**
     * 根据密钥生成的 SecretKey 对象
     * 使用 static final 避免重复生成，提高性能
     */
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * Token 过期时间：24 小时（毫秒值）
     * 86400000L = 24 * 60 * 60 * 1000
     * 使用 L 后缀表示 long 类型字面量，防止整数溢出
     */
    private static final long EXPIRATION = 86400000L; // 24小时

    /**
     * 生成 JWT Token
     *
     * 将用户 ID 和用户名编码到 Token 的载荷中。
     * Token 结构：
     * {
     *   "userId": 1,
     *   "username": "admin",
     *   "iat": 1712345678,     // 签发时间（issued at）
     *   "exp": 1712432078      // 过期时间（expiration）
     * }
     *
     * @param userId   用户 ID（通常来自数据库主键）
     * @param username 用户名（用于日志和调试）
     * @return JWT Token 字符串，格式：xxx.yyy.zzz
     */
    public static String createToken(Long userId, String username) {
        // Jwts.builder() 是 Builder 模式，链式调用设置 Token 的各项参数
        return Jwts.builder()
                // claims() 设置自定义载荷（Payload），以键值对 Map 的形式传入
                // 这里存放 userId 和 username，前端无法解密，但可以通过 parseToken 解析
                .claims(Map.of("userId", userId, "username", username))
                // issuedAt() 设置签发时间，即当前时间
                // 可用于判断 Token 的新旧，也可用于滑动过期策略
                .issuedAt(new Date())
                // expiration() 设置过期时间 = 当前时间 + 24 小时
                // 过期后 parseToken 会抛出 ExpiredJwtException
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                // signWith() 使用密钥对 Token 进行签名
                // 签名确保 Token 在传输过程中未被篡改
                .signWith(KEY)
                // compact() 完成构建，返回 Base64URL 编码的 Token 字符串
                .compact();
    }

    /**
     * 解析 JWT Token
     *
     * 验证 Token 的签名和有效期。如果验证通过，返回载荷中的 Claims 对象。
     * 如果验证失败，抛出对应的异常（ExpiredJwtException、SignatureException 等）。
     *
     * 使用方式：
     * Claims claims = JwtUtil.parseToken(token);
     * Long userId = claims.get("userId", Long.class);
     * String username = claims.get("username", String.class);
     *
     * @param token JWT Token 字符串
     * @return Claims 对象，包含 userId、username 等载荷信息
     * @throws ExpiredJwtException   Token 已过期
     * @throws MalformedJwtException Token 格式不正确
     * @throws SignatureException    Token 签名无效
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                // verifyWith() 设置验证签名的密钥
                // 必须是生成 Token 时使用的同一密钥，否则签名验证失败
                .verifyWith(KEY)
                // build() 构建解析器
                .build()
                // parseSignedClaims() 解析已签名的 Token 并返回载荷
                // 此方法会自动验证签名和有效期
                .parseSignedClaims(token)
                // getPayload() 获取载荷中的 Claims 数据
                .getPayload();
    }
}
```

**逐行讲解**：

- `Keys.hmacShaKeyFor()`：将字节数组转换为 HMAC-SHA256 密钥对象。密钥长度必须至少 256 位（32 字节），否则 jjwt 会抛出 `WeakKeyException`。

- `Map.of("userId", userId, "username", username)`：Java 9+ 的不可变 Map 创建方式。存放自定义载荷数据。注意不要存放敏感信息（如密码），因为载荷部分虽然经过 Base64 编码，但本质上是可解码的（不是加密）。

- `new Date(System.currentTimeMillis() + EXPIRATION)`：计算过期时间的常用写法。`System.currentTimeMillis()` 获取当前毫秒时间戳，加上 86400000（24小时）即为 24 小时后的时间戳。

- `Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token)`：链式 Builder 模式的典型应用。`verifyWith` 设置验证密钥，`build` 构建解析器，`parseSignedClaims` 执行解析。

### 4.3 AuthInterceptor.java — JWT 鉴权拦截器

文件路径：`bz_framework_core/src/main/java/com/linan/core/security/interceptor/AuthInterceptor.java`

```java
package com.linan.core.security.interceptor;

import com.linan.core.common.api.Result;
import com.linan.core.common.api.ResultCodeEnum;
import com.linan.core.security.JwtUtil;
import com.linan.core.security.PassAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 *
 * 拦截所有请求，验证 Authorization Header 中的 JWT Token。
 * 被 @PassAuth 标记的接口会直接放行，不需要携带 Token。
 *
 * Token 验证失败时，直接向 Response 写入 JSON 错误信息并阻断请求，
 * 不会将异常抛给 GlobalExceptionHandler（因为拦截器在 Controller 之前执行）。
 */
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    /*
     * Jackson 序列化工具，用于将 Result 对象转为 JSON 字符串写入 Response。
     * ObjectMapper 是线程安全的，可以作为静态常量复用。
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 请求前拦截
     *
     * 在请求到达 Controller 之前执行。返回 true 表示放行，false 表示拦截。
     *
     * 处理流程：
     * 1. 检查方法或类上是否有 @PassAuth 注解 -> 有则直接放行
     * 2. 从 Header 中提取 Authorization 字段 -> 为空则返回 401
     * 3. 解析 Token 并验证签名和有效期 -> 失败则返回 401
     * 4. 解析成功后，将 userId 和 username 存入 request 属性，供 Controller 使用
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 非 Controller 方法直接放行（静态资源、OPTIONS 等）
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查 @PassAuth 注解：方法级别优先于类级别
        if (handlerMethod.getMethodAnnotation(PassAuth.class) != null ||
            handlerMethod.getBeanType().getAnnotation(PassAuth.class) != null) {
            return true;
        }

        // 提取 Authorization Header
        String authHeader = request.getHeader("Authorization");

        // 格式校验：必须以 "Bearer " 开头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorizedResponse(response, "请求头中缺少Authorization信息");
            return false;
        }

        // 提取 Token（跳过 "Bearer " 前缀，长度为 7）
        String token = authHeader.substring(7);

        try {
            // 解析 Token，验证签名和有效期
            Claims claims = JwtUtil.parseToken(token);

            // 注入用户信息到 request，供后续 Controller 使用
            request.setAttribute("userId", claims.get("userId", Long.class));
            request.setAttribute("username", claims.get("username", String.class));

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            writeUnauthorizedResponse(response, "Token已过期，请重新登录");
            return false;

        } catch (SignatureException e) {
            log.warn("Token签名无效: {}", e.getMessage());
            writeUnauthorizedResponse(response, "Token签名无效");
            return false;

        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            writeUnauthorizedResponse(response, "Token格式错误");
            return false;

        } catch (Exception e) {
            log.error("Token解析异常: ", e);
            writeUnauthorizedResponse(response, "Token无效，请重新登录");
            return false;
        }
    }

    /**
     * 向 Response 写入 401 未授权的 JSON 响应
     *
     * 为什么直接写 Response 而不抛异常？
     * 拦截器在 Spring 的 DispatcherServlet 层面执行，
     * 在它之前 Controller 尚未执行，GlobalExceptionHandler 无法捕获拦截器抛出的异常。
     * 因此必须手动构造 JSON 响应并写入。
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            Result<Void> result = new Result<>(
                    ResultCodeEnum.UNAUTHORIZED.getCode(),
                    message != null ? message : ResultCodeEnum.UNAUTHORIZED.getMsg(),
                    null
            );

            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
            response.getWriter().flush();

        } catch (Exception e) {
            log.error("写入401响应失败: ", e);
        }
    }
}
```

**逐行讲解**：

- `implements HandlerInterceptor`：Spring 提供的拦截器接口，定义了三个方法：`preHandle`（请求前）、`postHandle`（请求后、视图渲染前）、`afterCompletion`（请求完成、视图渲染后）。鉴权只需实现 `preHandle`。

- `handler instanceof HandlerMethod`：`handler` 参数是处理请求的对象。如果是静态资源（如 CSS/JS），handler 不是 HandlerMethod，不需要鉴权。

- `handlerMethod.getMethodAnnotation(PassAuth.class)`：通过反射获取方法上的 @PassAuth 注解。`handlerMethod.getBeanType().getAnnotation(PassAuth.class)` 获取类上的注解。

- `request.getHeader("Authorization")`：标准的 HTTP 认证头。前端 Axios 拦截器会将 Token 设置在此 Header 中。

- `authHeader.substring(7)`："Bearer " 的长度正好是 7 个字符。字符串 `"Bearer eyJhbG..."` 截取从索引 7 开始的内容。

- `request.setAttribute("userId", ...)`：将解析出的用户信息放入 request 属性。Controller 中通过 `request.getAttribute("userId")` 获取当前用户 ID。

- `response.getWriter().write(json)`：使用 Servlet API 的 Writer 直接向响应输出 JSON。设置 Content-Type 后，浏览器/前端能正确解析。

---

## 5. 将拦截器注册到 Spring MVC（在 WebMvcConfigurer 中配置）

AuthInterceptor 编写完成后，必须在 Spring 的 WebMvcConfigurer 中注册才能生效。具体配置见下一章 [04-Web配置与跨域处理](./04-Web配置与跨域处理.md)。

简要示例：

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")    // 拦截所有 /api/ 路径
                .excludePathPatterns("/api/auth/v1/login");  // 排除登录接口
    }
}
```

---

## 6. 在 Controller 中使用鉴权功能

### 6.1 公开接口（无需 Token）

```java
@PassAuth
@RestController
@RequestMapping("/api/auth/v1")
public class AuthController {

    @PassAuth  // 方法级别显式标记（类上已有，此处可省略）
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDto dto) {
        // 验证用户名密码
        // 生成 Token
        String token = JwtUtil.createToken(user.getUserId(), user.getUserName());
        return Result.success(token);
    }
}
```

### 6.2 需要登录的接口

```java
@RestController
@RequestMapping("/api/system/v1/users")
public class UserController {

    @GetMapping
    public Result<List<User>> listUsers(HttpServletRequest request) {
        // 从 request 属性中获取当前用户信息（由 AuthInterceptor 注入）
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        log.info("用户 {} (ID:{}) 查询用户列表", username, userId);
        // ... 业务逻辑
    }
}
```

### 6.3 获取当前登录用户（便捷方式）

可以创建一个工具类来简化获取当前用户的操作：

```java
package com.linan.core.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全上下文工具类
 * 用于便捷获取当前登录用户信息
 */
public class SecurityContextHolder {

    /**
     * 获取当前登录用户 ID
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }
}
```

---

## 7. 接口交互示例（前后端对接参考）

### 7.1 登录流程

```text
# 1. 前端发送登录请求
POST /api/auth/v1/login
Content-Type: application/json
Body: {"username":"admin","password":"123456"}

# 2. 后端验证用户名密码，生成 Token
Response: {"code":200,"msg":"操作成功","data":"eyJhbGciOiJIUzI1NiJ9.xxx"}

# 3. 前端存储 Token 到 localStorage
localStorage.setItem("token", response.data)

# 4. 前端发送带 Token 的请求
GET /api/system/v1/users
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx

# 5. AuthInterceptor 解析 Token，注入用户信息后放行
```

### 7.2 Token 过期处理

```text
# 1. Token 已过期，前端发送请求
GET /api/system/v1/users
Authorization: Bearer <expired_token>

# 2. AuthInterceptor 捕获 ExpiredJwtException
Response: {"code":401,"msg":"Token已过期，请重新登录","data":null}

# 3. 前端 Axios 响应拦截器检测到 code=401
# 清除 localStorage 中的 token
# 跳转到登录页面
```

---

## 8. Token 过期策略

### 8.1 当前策略：固定过期时间

Token 签发后 24 小时内有效，超时后必须重新登录。

优点：实现简单，无需额外存储。
缺点：用户体验不友好——如果用户在 23:59 分操作，下一分钟就需要重新登录。

### 8.2 可选的滑动过期策略

如果需要更好的用户体验，可以实现滑动过期——每次请求成功后自动刷新 Token 的有效期。

在 AuthInterceptor 的 `preHandle` 中，解析成功后生成新的 Token 并放入响应头：

```java
// 在 preHandle 的 try 块末尾添加
String newToken = JwtUtil.createToken(
    claims.get("userId", Long.class),
    claims.get("username", String.class)
);
response.setHeader("New-Token", newToken);
```

前端 Axios 响应拦截器检测到 `New-Token` 响应头后，自动更新 localStorage 中的 Token。

---

## 9. 安全注意事项

### 9.1 密钥管理

当前 `SECRET` 硬编码在代码中，仅适用于本地开发。生产环境必须改用环境变量或配置中心：

```java
// 推荐方式：从环境变量读取
private static final String SECRET = System.getenv("JWT_SECRET") != null
    ? System.getenv("JWT_SECRET")
    : "default-dev-secret";  // 仅开发环境使用默认值
```

### 9.2 Token 载荷安全

JWT 的载荷部分经过 Base64 编码，但**不是加密**。任何拿到 Token 的人都可以解码查看载荷内容。因此：
- 不要在载荷中存放敏感信息（如密码、手机号、身份证号）
- 载荷中仅存放必要的标识信息（userId、username）

### 9.3 HTTPS 传输

生产环境必须使用 HTTPS 传输，因为 Token 在 HTTP 请求头中是明文传输的。

---

## 10. 测试验证

### 10.1 测试公开接口

```bash
# 无需 Token 即可访问
curl http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 10.2 测试需要 Token 的接口

```bash
# 不带 Token：返回 401
curl http://localhost:8080/api/system/v1/users
# 响应：{"code":401,"msg":"请求头中缺少Authorization信息","data":null}

# 带正确 Token：正常返回
curl http://localhost:8080/api/system/v1/users \
  -H "Authorization: Bearer <先通过登录获取的token>"

# 带错误 Token：返回 401
curl http://localhost:8080/api/system/v1/users \
  -H "Authorization: Bearer invalid_token"
# 响应：{"code":401,"msg":"Token格式错误","data":null}
```

---

## 11. 本章完成检查清单

- [ ] 理解 JWT 的三部分结构（Header、Payload、Signature）
- [ ] 理解 `JwtUtil.createToken()` 和 `parseToken()` 的工作原理
- [ ] 理解 `@PassAuth` 注解的 `@Target` 和 `@Retention` 设置
- [ ] 理解 AuthInterceptor 中 `preHandle` 的处理流程
- [ ] 理解为什么拦截器中不能直接抛异常给 GlobalExceptionHandler
- [ ] 理解 Token 过期后前端的处理流程
- [ ] 理解密钥安全管理的重要性

---

*下一章：[04-Web配置与跨域处理](./04-Web配置与跨域处理.md)*
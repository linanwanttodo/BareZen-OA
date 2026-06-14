# 05-SpringBoot主启动类编写

## 1. 功能概述

本章完成 BareZen_OA 后端的启动入口配置，使项目从一个编译通过的空壳变为可以启动的 Spring Boot 应用。

本章将创建/修改以下内容：
- **主启动类**：Spring Boot 应用入口，整合两个模块的组件扫描
- **构建脚本**：bz_web_app 的 `build.gradle.kts`，添加核心模块依赖和 Spring Boot 插件
- **配置文件**：`application.yml`，配置服务端口、JSON 序列化、日志等

---

## 2. 设计原理

### 2.1 多模块项目的组件扫描

在多模块 Gradle 项目中，不同模块的代码分布在不同的包中：

```text
bz_framework_core -> com.linan.core.*     (核心底座)
bz_web_app        -> com.linan.web.*      (业务启动)
```

Spring Boot 的默认行为是以主启动类所在包为基准进行组件扫描。如果启动类在 `com.linan.web`，Spring 只会扫描 `com.linan.web` 及其子包，不会扫描 `com.linan.core.*` 中的组件（拦截器、配置类等）。

因此必须通过 `scanBasePackages` 参数显式指定需要扫描的包：

```java
@SpringBootApplication(scanBasePackages = {
    "com.linan.core",  // 核心底座模块
    "com.linan.web"    // 当前业务模块
})
```

### 2.2 Spring Boot 插件的作用

在 `bz_web_app/build.gradle.kts` 中应用 `spring-boot` 插件可激活以下任务：

| 任务 | 说明 |
| :--- | :--- |
| `bootRun` | 启动 Spring Boot 应用（开发模式，支持热重载） |
| `bootJar` | 打包为可执行 JAR 文件（包含所有依赖） |
| `bootBuildImage` | 构建 Docker 镜像（使用 Cloud Native Buildpacks） |

### 2.3 配置文件层次

Spring Boot 支持多种格式的配置文件：

| 格式 | 优先级 | 说明 |
| :--- | :---: | :--- |
| `application.yml` | 低 | 标准配置，纳入版本控制 |
| `application-dev.yml` | 中 | 开发环境配置，通过 `spring.profiles.active=dev` 激活 |
| `application-prod.yml` | 中 | 生产环境配置 |
| 环境变量 | 高 | 最高优先级，如 `SERVER_PORT=9090` |
| 命令行参数 | 最高 | `--server.port=9090` |

---

## 3. 文件清单

| 文件路径 | 说明 |
| :--- | :--- |
| `bz_web_app/src/main/java/com/linan/web/BareZenApplication.java` | Spring Boot 主启动类 |
| `bz_web_app/build.gradle.kts` | Web 应用模块构建脚本（更新版） |
| `bz_web_app/src/main/resources/application.yml` | 应用配置文件 |

---

## 4. 代码实现

### 4.1 BareZenApplication.java — 主启动类

文件路径：`bz_web_app/src/main/java/com/linan/web/BareZenApplication.java`

```java
package com.linan.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BareZen OA 后端应用主启动类
 *
 * 这是整个 Spring Boot 应用的入口点。使用 @SpringBootApplication 注解
 * 自动启用组件扫描、自动配置和属性支持。
 *
 * 组件扫描说明：
 * 由于主启动类位于 com.linan.web 包中，Spring Boot 默认只扫描当前包及其子包。
 * 核心底座模块（bz_framework_core）的代码位于 com.linan.core 包中，
 * 需要显式指定 scanBasePackages 参数来告知 Spring 扫描两个模块的包。
 *
 * 启动方式：
 *   # 命令行（在 backend 目录下）：
 *   ./gradlew :bz_web_app:bootRun
 *   # 或使用 Makefile：
 *   make dev-api
 *   # 或在 IDEA 中直接运行此类的 main 方法
 *
 * @author BareZen_OA Team
 * @since 1.0-SNAPSHOT
 */
@SpringBootApplication(scanBasePackages = {
        "com.linan.core",  // 扫描核心底座模块的组件（config、security 等）
        "com.linan.web"    // 扫描当前模块的组件（controller、service 等）
})
public class BareZenApplication {

    /**
     * 应用启动入口
     *
     * SpringApplication.run() 负责：
     * 1. 创建 Spring 应用上下文（ApplicationContext）
     * 2. 执行自动配置（AutoConfiguration）
     * 3. 启动内嵌的 Tomcat 服务器（默认端口 8080）
     * 4. 注册所有 Bean（Controller、Service、拦截器等）
     * 5. 完成应用启动
     *
     * @param args 命令行参数（可以通过 --server.port=9090 等方式覆盖配置）
     */
    public static void main(String[] args) {
        SpringApplication.run(BareZenApplication.class, args);
    }
}
```

**逐行讲解**：

- `@SpringBootApplication`：这是一个组合注解，等价于同时使用以下三个注解：
  - `@SpringBootConfiguration`（标记为配置类，等价于 `@Configuration`）
  - `@EnableAutoConfiguration`（启用 Spring Boot 自动配置）
  - `@ComponentScan`（启用组件扫描）

- `scanBasePackages = {"com.linan.core", "com.linan.web"}`：显式指定需要扫描的包名数组。没有此参数时，Spring 只扫描 `com.linan.web` 包。加上此参数后，Spring 会扫描两个模块的所有组件（`@Configuration`、`@Controller`、`@Service`、`@Component`、拦截器注册等）。

- `SpringApplication.run(BareZenApplication.class, args)`：启动 Spring Boot 应用的静态方法。内部做了大量初始化工作，包括创建 ApplicationContext、扫描组件、注册 Bean、启动 Web 服务器等。

### 4.2 build.gradle.kts — 构建脚本更新

文件路径：`bz_web_app/build.gradle.kts`

```kotlin
plugins {
    id("java")
    // 应用 Spring Boot 插件，提供 bootRun、bootJar 等任务
    // 注意：version 已在根构建脚本中声明，此处无需重复
    id("org.springframework.boot")
}

group = "com.linan"
version = "1.0-SNAPSHOT"

dependencies {
    // 引入核心底座模块（提供 Result、JWT、异常处理等基础能力）
    // project() 是 Gradle 的多模块依赖声明方式
    // bz_framework_core 中的依赖（如 jjwt）会传递到此模块
    implementation(project(":bz_framework_core"))

    // Spring Boot Web 启动器（嵌入式 Tomcat + Spring MVC + Jackson）
    // 版本号由根构建脚本中的 BOM 统一管理
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 测试依赖
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Spring Boot 测试支持（提供 @SpringBootTest 等注解）
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
```

**关键变化说明**：

相比初始版本，此版本新增了：
1. `id("org.springframework.boot")`：应用 Spring Boot 插件，提供 `bootRun` 任务
2. `implementation(project(":bz_framework_core"))`：引入核心底座模块依赖
3. `implementation("org.springframework.boot:spring-boot-starter-web")`：引入 Web 启动器
4. `testImplementation("org.springframework.boot:spring-boot-starter-test")`：引入测试支持

### 4.3 application.yml — 应用配置

文件路径：`bz_web_app/src/main/resources/application.yml`

```yaml
# ============================================
# BareZen OA 应用配置
# ============================================

# ---- 服务端口 ----
server:
  port: 8080

# ---- Spring 核心配置 ----
spring:
  application:
    name: BareZen_OA         # 应用名称

  # ---- Jackson JSON 序列化配置 ----
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss   # 全局日期格式
    time-zone: Asia/Shanghai            # 时区
    serialization:
      write-dates-as-timestamps: false  # 不使用时间戳格式

# ---- JWT 配置 ----
jwt:
  expiration: 86400000  # Token 过期时间（毫秒），24 小时

# ---- 日志配置 ----
logging:
  level:
    root: INFO                        # 根日志级别
    com.linan: DEBUG                  # 项目包日志级别
    org.springframework: WARN         # Spring 框架日志级别
```

**逐行讲解**：

- `server.port: 8080`：应用监听的 HTTP 端口号。Spring Boot 默认也是 8080，显式声明便于理解和修改。
- `spring.application.name: BareZen_OA`：应用名称，用于日志输出、Actuator 端点、Spring Cloud 服务注册等。
- `spring.jackson.date-format: yyyy-MM-dd HH:mm:ss`：Java 的 Date/LocalDateTime 对象序列化为 JSON 时使用的格式。
- `write-dates-as-timestamps: false`：false 表示使用字符串格式输出日期（如 "2026-05-05 14:30:00"），true 表示使用毫秒时间戳（如 1714888200000）。
- `logging.level.com.linan: DEBUG`：设置项目代码的日志级别为 DEBUG，开发阶段能看到更多的调试信息。

---

## 5. 启动应用

### 5.1 通过 Gradle 启动

```bash
cd backend

# 方式一：启动 bz_web_app 模块
./gradlew :bz_web_app:bootRun

# 方式二：使用 Makefile
make dev-api
```

### 5.2 预期启动输出

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.4)

2026-05-05 14:30:00 [main] INFO  com.linan.web.BareZenApplication - Starting BareZenApplication ...
2026-05-05 14:30:02 [main] INFO  o.s.b.w.embedded.tomcat.TomcatWebServer - Tomcat started on port 8080
2026-05-05 14:30:03 [main] INFO  com.linan.web.BareZenApplication - Started BareZenApplication in 3.2 seconds
```

### 5.3 验证应用正在运行

```bash
# 测试端口是否被监听
curl http://localhost:8080/

# 预期响应：
# {"code":404,"msg":"请求路径不存在","data":null}
# （因为我们还没有编写任何 Controller，所以返回 404）
```

---

## 6. 在 IDEA 中直接运行

1. 打开 `BareZenApplication.java`
2. 点击 `main` 方法左侧的绿色三角形图标（或右键 -> Run 'BareZenApplication'）
3. IDEA 底部控制台将显示启动日志

**IDEA 中的优势**：
- 可以直接在代码中设置断点调试
- 修改代码后可以通过 `Run -> Reload Changed Classes` 热重载
- 控制台集成了请求日志的显示

---

## 7. 第一个 Controller：健康检查接口

为了让应用有一个可访问的接口来验证启动是否成功，添加一个简单的健康检查接口：

```java
package com.linan.web.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.PassAuth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口
 * 用于验证应用是否正常运行
 */
@PassAuth  // 不需要登录即可访问
@RestController
@RequestMapping("/api/common/v1")
public class HealthController {

    /**
     * 健康检查
     * 请求：GET /api/common/v1/health
     * 响应：{"code":200,"msg":"服务运行正常","data":"BareZen_OA v1.0-SNAPSHOT"}
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("服务运行正常", "BareZen_OA v1.0-SNAPSHOT");
    }
}
```

创建此文件后，使用 curl 验证：

```bash
curl http://localhost:8080/api/common/v1/health
# 响应：{"code":200,"msg":"服务运行正常","data":"BareZen_OA v1.0-SNAPSHOT"}
```

---

## 8. 常见问题

**Q: 启动时报 `WebMvcConfig` 找不到 `AuthInterceptor`？**

A: 确保 `@SpringBootApplication` 的 `scanBasePackages` 包含了 `"com.linan.core"`。如果缺少，Spring 不会扫描核心模块的组件。

**Q: 启动时报 `Unable to start web server`？**

A: 原因可能是端口 8080 被占用。可以更改端口：在 `application.yml` 中修改 `server.port`，或启动时指定 `--server.port=9090`。

**Q: bz_framework_core 中的依赖找不到（如 jjwt）？**

A: 检查 bz_web_app 是否引入了 `implementation(project(":bz_framework_core"))`。由于 core 模块使用的是 `implementation` 配置（非 `api`），其依赖不会自动传递。如果需要在 web_app 中直接使用 jjwt，需要显式添加依赖。

**Q: @SpringBootApplication 和 @Configuration + @ComponentScan 有什么区别？**

A: `@SpringBootApplication` 包含了 `@EnableAutoConfiguration`，这是 Spring Boot 自动配置的核心。如果仅使用 `@Configuration` + `@ComponentScan`，Spring Boot 的自动配置不会启用，需要手动配置很多东西。

---

## 9. 核心底座阶段总结

到目前为止，我们已经完成了 BareZen_OA 后端的核心底座（bz_framework_core）和启动入口（bz_web_app）的所有代码和配置。回顾已完成的组件：

| 组件 | 文件 | 功能 |
| :--- | :--- | :--- |
| 统一返回体 | Result.java, ResultCodeEnum.java | 标准化所有接口的响应格式 |
| 全局异常处理 | GlobalExceptionHandler.java | 统一拦截异常并转换为 JSON 响应 |
| JWT 工具类 | JwtUtil.java | Token 生成与解析 |
| 跳过鉴权注解 | PassAuth.java | 标记公开接口 |
| 鉴权拦截器 | AuthInterceptor.java | 请求级别 Token 验证 |
| Web 配置 | WebMvcConfig.java | 拦截器注册 + CORS 跨域 |
| 主启动类 | BareZenApplication.java | Spring Boot 应用入口 |
| 应用配置 | application.yml | 服务端口、JSON、日志等配置 |

**此时启动应用后，已经具备以下能力**：
- 统一的 JSON 响应格式
- 自动的异常处理（不会泄露内部信息给前端）
- JWT 身份认证（未登录请求返回 401）
- 跨域支持（前端可以正常调用接口）

**下一步**：集成 MyBatis-Plus 数据库持久层，开始编写业务代码。

---

## 10. 本章完成检查清单

- [ ] `BareZenApplication.java` 已创建，包含正确的 `scanBasePackages`
- [ ] `bz_web_app/build.gradle.kts` 已更新，包含 core 模块依赖和 Spring Boot 插件
- [ ] `application.yml` 已创建，配置了基本参数
- [ ] 启动应用后能在 `http://localhost:8080/api/common/v1/health` 看到健康检查响应
- [ ] 不带 Token 访问 `/api/system/v1/users` 返回 `{"code":401}`

---

*核心底座开发完成！下一章进入 [03-持久层集成](../03-持久层集成/01-MyBatis-Plus整合配置.md)*
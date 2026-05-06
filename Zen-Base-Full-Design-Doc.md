# BareZen_Base (BareZen_Base) 极简模块化全栈框架详细设计开发文档

## 1. 项目背景与愿景

### 1.1 为什么发起 BareZen_Base？
在当前 Java 开发生态中，以若依（RuoYi）为代表的后台框架虽然功能强大，但对于现代个人开发者和追求极致效率的小型团队来说，存在以下痛点：
* **过度耦合**：权限系统、部门管理等业务逻辑与核心底座深度绑定，难以剥离。
* **技术包袱**：大量使用 Java 8 架构，未能利用现代 Java 特性。
* **心智负担**：为了实现一个简单的功能，需要理解框架内部复杂的路由和鉴权逻辑。

**BareZen_Base 的目标**是打造一个“微内核 + 插件”模式的架构，既能作为极简的工具底座，也能通过插件扩展支持企业级复杂业务。

### 1.2 BareZen_Base vs RuoYi 深度对标
| 特性 | RuoYi (若依) | BareZen_Base (BareZen_Base) |
| :--- | :--- | :--- |
| **设计哲学** | 大而全，开箱即用 | 极致轻量，按需组装 |
| **构建工具** | Maven (XML 配置较重) | Gradle Kotlin DSL (高性能且极客) |
| **Java 版本** | 主要是 Java 8 | **Java 21 (强制，利用虚拟线程)** |
| **鉴权模型** | Spring Security (极其臃肿) | **纯净 JWT 拦截器 (轻量可控)** |
| **前端架构** | 单一 Admin 工程 | **一后带两前 (Admin端 + User端)** |
| **学习曲线** | 陡峭 (需学习框架私有逻辑) | 平滑 (遵循标准 Spring Boot / Vue 开发) |

---

## 2. 核心技术栈规划

### 2.1 后端 (Backend)
* **核心框架**：Spring Boot 3.2+ (Jakarta EE 10)
* **运行时**：Java 21 (利用虚拟线程 Virtual Threads 压榨 I/O 性能)
* **持久层**：MyBatis-Plus 3.5.5+
* **数据库**：MySQL 8.0+ / Redis 7.0
* **鉴权机制**：轻量级 JWT (jjwt 0.12+)

### 2.2 前端 (Frontend)
* **构建工具**：Vite 5.x
* **核心框架**：Vue 3.4+ (Composition API)
* **脚本语言**：TypeScript 5.x
* **状态管理**：Pinia
* **UI 组件库**：Element Plus (管理端) / Tailwind CSS (用户端)

---

## 3. 系统架构与目录结构

### 3.1 模块化设计原则
采用 Maven/Gradle 多模块结构，各功能通过依赖引入，不引入即不生效。
* **`bz_framework_core`**：微内核，提供统一返回体、异常捕获、JWT 基础（RBAC、存储抽象等插件能力已合入本模块）。
* **`framework-rbac`**：权限插件，提供用户、角色、菜单、部门逻辑。
* **`framework-oss`**：存储插件，提供对象存储接入。
* **`web-admin`**：主启动类，负责业务 Controller 的承载。

### 3.2 详细目录树
```text
BareZen_Base/
├── backend/                       # 后端根工程（Gradle 多模块）
│   ├── build.gradle.kts           # 全局依赖管理
│   ├── settings.gradle.kts        # 模块包含逻辑
│   ├── bz_framework_core/         # 【微内核】核心逻辑（含 RBAC/系统/HR/办公/资产/知识库域）
│   │   └── src/main/java/com/linan/core/
│   │       ├── common/api/        # 统一返回体 Result
│   │       ├── common/exception/  # 全局异常处理
│   │       ├── security/          # JWT 拦截器逻辑
│   │       └── config/            # 跨域与 JSON 配置
│   └── bz_web_app/                # 【主启动】业务应用入口（Controller/配置/Flyway 迁移）
│
├── frontend-admin/                # 管理端 (Vue 3 + Arco Design)
│   ├── src/api/                   # 对接 /api/system/*、/api/hr/* 等
│   ├── src/layout/                # 管理后台侧边栏布局
│   └── src/views/                 # 具体业务管理页面
│
├── frontend-user/                 # 用户前台 (Vue 3 + Tailwind CSS)
│   ├── src/api/                   # 对接 /api/app/*
│   └── src/views/                 # 展示类页面
│
├── Makefile                       # 极客工具 (make dev-api 等)
└── docker-compose.yml             # MySQL/Redis 环境编排
```

---

## 4. 开发落地执行手册 (SOP)

### 第一阶段：基础设施 (Day 1)
1. **初始化 Gradle 项目**：配置 `build.gradle.kts` 版本和多模块 `include`。
2. **环境预热**：通过 Docker 启动数据库。
3. **编写脚本**：配置 `Makefile` 以实现一键启动。

### 第二阶段：核心底座开发 (Day 2-3)
1. **统一接口**：编写 `Result` 和 `ResultCodeEnum`。
2. **构建异常防线**：实现 `GlobalExceptionHandler`。
3. **鉴权闭环**：完成 JWT 生成工具类与 `HandlerInterceptor` 拦截逻辑。

### 第三阶段：RBAC 权限插件 (Day 4-5)
1. **数据库建模**：设计无物理外键的五张权限核心表。
2. **动态鉴权**：利用 Spring AOP 结合自定义注解实现接口级权限控制。

### 第四阶段：前端双端骨架 (Day 6-7)
1. **双端构建**：使用 Vite 分别初始化 Admin 端和 User 端。
2. **深度集成**：封装 Axios 拦截器，统一处理 401 自动跳转登录。

---

## 5. 核心初始化源码清单 (部分)

### 5.1 统一返回体规范 (`bz_framework_core`)
```java
package com.linan.core.common.api;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
}
```

### 5.2 全局异常拦截 (`bz_framework_core`)
```java
package com.linan.core.common.exception;

import com.linan.core.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统运行异常: ", e);
        return Result.error("服务器异常，请联系管理员");
    }
}
```

### 5.3 构建配置 (`build.gradle.kts`)
```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.2.4" apply false
}

allprojects {
    group = "com.linan"
    version = "1.0.0"
    repositories {
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
    }
}

subprojects {
    apply(plugin = "java")
    java.sourceCompatibility = JavaVersion.VERSION_21
    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
    }
}
```

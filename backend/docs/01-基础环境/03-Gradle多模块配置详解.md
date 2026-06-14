# 03-Gradle多模块配置详解

## 1. 概述

BareZen_OA 后端采用 Gradle 多模块（Multi-Module）架构，将不同职责的代码分离到独立模块中。本章详细讲解 Gradle Kotlin DSL（`.kts`）配置的原理和最佳实践。

完成本章后，你将理解：
- 为什么选择 Gradle 而非 Maven
- Kotlin DSL 相比 Groovy DSL 的优势
- 多模块项目的依赖管理策略
- 如何新增子模块并正确配置依赖

---

## 2. 为什么选择 Gradle？

### 2.1 Gradle vs Maven 对比

| 特性 | Maven | Gradle |
| :--- | :--- | :--- |
| 构建脚本 | XML（`pom.xml`），冗长且难以复用 | Kotlin/Groovy DSL，简洁且可编程 |
| 构建速度 | 每次全量执行生命周期 | 增量构建 + 构建缓存，显著更快 |
| 依赖管理 | `dependencyManagement` + 继承 | `platform(BOM)` + 约束，更灵活 |
| 多模块 | 聚合 + 继承，XML 配置繁重 | 脚本式配置，`subprojects` 统一管理 |
| IDE 支持 | 成熟稳定 | IDEA 原生支持，体验优秀 |
| 学习成本 | 较低（约定优于配置） | 中等（需要理解 DSL 和 Task 体系） |

### 2.2 Kotlin DSL vs Groovy DSL

BareZen_OA 使用 Kotlin DSL（`.kts` 文件）而非 Groovy DSL（`.gradle` 文件），原因如下：

- **类型安全**：Kotlin 是静态类型语言，IDE 可提供自动补全和编译时检查，Groovy 是动态类型。
- **更好的 IDE 支持**：IDEA 对 Kotlin 的支持优于 Groovy，代码导航和重构更可靠。
- **语言一致性**：如果项目中使用了 Kotlin（如 Spring Boot + Kotlin），构建脚本也用 Kotlin 可降低认知负担。

```kotlin
// Kotlin DSL (.kts) - 类型安全，IDE 友好
java {
    sourceCompatibility = JavaVersion.VERSION_21  // 枚举类型有提示
}

// Groovy DSL (.gradle) - 动态类型，运行时才发现错误
java {
    sourceCompatibility = JavaVersion.VERSION_21
}
```

---

## 3. Gradle Wrapper 机制

### 3.1 什么是 Gradle Wrapper

`gradlew`（Linux/macOS）和 `gradlew.bat`（Windows）是 Gradle Wrapper 脚本。它的作用是：

- 项目自带 Gradle 版本声明，无需开发者手动安装 Gradle
- 首次执行时自动下载指定版本的 Gradle
- 确保所有开发人员使用相同的 Gradle 版本

### 3.2 Wrapper 配置文件

文件路径：`backend/gradle/wrapper/gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

关键配置：
- `distributionUrl`：声明 Gradle 的下载地址和版本。当前使用 `8.5` 版本。
- 首次运行 `./gradlew` 时，会下载此 URL 中的 ZIP 包并缓存到 `~/.gradle/`。

### 3.3 更新 Gradle 版本

如果需要升级 Gradle 版本：

```bash
# 方式一：执行 wrapper 任务
./gradlew wrapper --gradle-version 8.7

# 方式二：直接修改 gradle-wrapper.properties 中的 URL
```

---

## 4. settings.gradle.kts 详解

文件路径：`backend/settings.gradle.kts`

```kotlin
rootProject.name = "BareZen_OA"

// ---- 子模块声明 ----
// 每个 include 声明一个子模块，名称为模块目录名
include("bz_framework_core")  // 核心底座模块
include("bz_web_app")         // Web 应用启动模块

// ---- 插件管理 ----
// Gradle 插件自身的下载仓库（如 spring-boot 插件）
pluginManagement {
    repositories {
        // 阿里云 Gradle 插件镜像（加速国内下载）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // gradlePluginPortal()  // 官方仓库，如果需要阿里云没有的插件可放开
    }
}

// ---- 依赖管理 ----
// 项目依赖（如 spring-boot-starter-web）的下载仓库
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}
```

### 4.1 rootProject.name

```kotlin
rootProject.name = "BareZen_OA"
```
设置根项目名称，该名称会出现在 Gradle 面板、构建输出和 IDEA 导航中。

### 4.2 include()

```kotlin
include("bz_framework_core")
include("bz_web_app")
```
声明项目包含的子模块。每个 `include` 对应一个与 `settings.gradle.kts` 同级的下级目录。Gradle 会在对应目录中查找 `build.gradle.kts`。

**新增子模块只需两步**：
1. 创建模块目录并编写 `build.gradle.kts`
2. 在此文件中添加 `include("新模块名")`

### 4.3 pluginManagement

`pluginManagement` 用于配置 Gradle 插件自身的解析仓库。Gradle 插件（如 `org.springframework.boot`）是通过 `plugins {}` 块声明的，它们有独立的仓库配置。

阿里云镜像用于加速国内下载，官方仓库 `gradlePluginPortal()` 作为备选。

### 4.4 dependencyResolutionManagement

`dependencyResolutionManagement` 用于集中管理所有模块的依赖下载仓库，避免在每个子模块中重复配置。

这里的配置会覆盖子模块中的 `repositories {}` 声明，确保所有依赖从统一的仓库下载。

---

## 5. build.gradle.kts（根构建脚本）详解

文件路径：`backend/build.gradle.kts`

### 5.1 完整脚本

```kotlin
// ============================================
// 插件声明
// ============================================
plugins {
    id("java")
    // Spring Boot 插件：apply false 表示仅声明，不应用于根项目
    id("org.springframework.boot") version "3.2.4" apply false
    // Spring 依赖管理插件：让子模块可以省略版本号
    id("io.spring.dependency-management") version "1.1.7" apply false
    // Kotlin JVM 插件：解决 IDEA 的 prepareKotlinBuildScriptModel 错误
    kotlin("jvm") version "1.9.0" apply false
}

// ============================================
// 全局坐标
// ============================================
group = "com.linan"
version = "1.0-SNAPSHOT"

// ============================================
// 所有项目（含根项目）的仓库
// ============================================
allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

// ============================================
// 所有子模块的公共配置
// ============================================
subprojects {
    // 应用 Java 插件（提供 compileJava 等任务）
    apply(plugin = "java")
    // 应用 Spring 依赖管理插件（让子模块可以省略版本号）
    apply(plugin = "io.spring.dependency-management")

    // 引入 Spring Boot BOM，统一管理 Spring 生态依赖版本
    dependencies {
        implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
    }

    // 强制 Java 21
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // 公共依赖（所有子模块都需要的库）
    dependencies {
        // Lombok：编译时生成 getter/setter/构造器
        // compileOnly：编译时需要，运行时不需要（Lombok 只在编译期工作）
        // annotationProcessor：注解处理器，编译时生成代码
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // 测试相关
        testImplementation(platform("org.junit:junit-bom:6.0.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    // 测试任务配置
    tasks.test {
        useJUnitPlatform()  // 使用 JUnit 5 平台运行测试
    }
}
```

### 5.2 plugins {} 块详解

```kotlin
plugins {
    id("java")
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "1.9.0" apply false
}
```

- `id("java")`：Java 插件，提供 `compileJava`、`test` 等标准任务。应用于根项目。
- `apply false`：这是关键。它表示仅声明插件的存在和版本，但**不应用**到当前项目。子模块需要时可以单独 `apply`。
- `kotlin("jvm")`：Kotlin JVM 插件，用于解决 IDEA 的 `prepareKotlinBuildScriptModel` 错误。

**为什么 spring-boot 插件用 apply false？**
因为根项目不是 Spring Boot 应用，只有 `bz_web_app` 是启动入口。但我们需要在根项目声明版本号，让子模块直接引用。

### 5.3 allprojects vs subprojects

```kotlin
allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

subprojects {
    apply(plugin = "java")
    // ...
}
```

| 作用域 | 范围 | 用途 |
| :--- | :--- | :--- |
| `allprojects` | 根项目 + 所有子模块 | 配置所有项目都需要的设置（如仓库） |
| `subprojects` | 仅子模块 | 配置子模块的公共依赖和任务 |

### 5.4 BOM 依赖管理

```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
}
```

BOM（Bill of Materials）是一个特殊的 POM 文件，定义了一组兼容的依赖版本。引入 BOM 后，子模块可以在使用相关依赖时省略版本号：

```kotlin
// 无需指定版本号，Gradle 会从 BOM 中查找
implementation("org.springframework.boot:spring-boot-starter-web")
```

这确保所有模块使用相同版本的 Spring 库，避免版本冲突。

### 5.5 Java 版本配置

```kotlin
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

- `sourceCompatibility`：源代码使用的 Java 版本，编译器按此版本检查语法
- `targetCompatibility`：编译生成的字节码版本，确保兼容目标 JVM

这两项都设置为 21，表示项目使用 Java 21 的全部特性（如虚拟线程、Record 模式、Switch 表达式增强等）。

---

## 6. 子模块构建脚本详解

### 6.1 bz_framework_core 模块

文件路径：`backend/bz_framework_core/build.gradle.kts`

```kotlin
plugins {
    id("java")  // Java 插件（尽管根脚本已应用，显式声明更清晰）
}

group = "com.linan"
version = "1.0-SNAPSHOT"

dependencies {
    // ============================================
    // Spring Boot Web 基础依赖
    // 版本号由根脚本的 BOM 统一管理，无需在此指定
    // ============================================
    implementation("org.springframework.boot:spring-boot-starter-web")
    // spring-boot-starter-web 包含：
    // - spring-boot-starter（核心启动器）
    // - spring-boot-starter-json（Jackson JSON 处理）
    // - spring-boot-starter-tomcat（嵌入式 Tomcat）
    // - spring-web（Spring MVC 框架）
    // - spring-webmvc（Spring Web MVC）

    // ============================================
    // Lombok 简化代码
    // 明确指定版本号 1.18.34 以完美支持 JDK 21
    // ============================================
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // ============================================
    // JWT 支持（jjwt 0.12.x）
    // 版本号未在 BOM 中管理，需要显式指定
    // ============================================
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    // 三个模块的职责：
    // - jjwt-api：JWT 核心 API 接口
    // - jjwt-impl：JWT 实现（运行时需要，编译时不需要）
    // - jjwt-jackson：使用 Jackson 序列化（运行时需要）
    // 模块拆分的好处：编译时仅依赖 API，运行时可灵活替换实现

    // 测试依赖
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

### 6.2 bz_web_app 模块（当前状态）

文件路径：`backend/bz_web_app/build.gradle.kts`

```kotlin
plugins {
    id("java")
}

group = "com.linan"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

**当前状态**：该模块仅配置了测试依赖，尚未添加对 `bz_framework_core` 的依赖和 Spring Boot 启动配置。我们将在后续章节中逐步完善它。

后续完善后的 `bz_web_app/build.gradle.kts` 将包含：

```kotlin
plugins {
    id("java")
    id("org.springframework.boot")  // 应用 Spring Boot 插件（提供 bootRun 等任务）
}

dependencies {
    // 引入核心底座模块
    implementation(project(":bz_framework_core"))

    // 引入 Spring Boot 启动器
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

---

## 7. 依赖配置类型说明

Gradle 提供了多种依赖配置（Configuration），理解它们的区别很重要：

| 配置 | 编译可用 | 运行可用 | 传递依赖 | 说明 |
| :--- | :---: | :---: | :---: | :--- |
| `implementation` | 是 | 是 | 否 | **推荐使用**。依赖对消费者隐藏，构建更快 |
| `api` | 是 | 是 | 是 | 依赖暴露给消费者，类似 Maven 的 compile |
| `compileOnly` | 是 | 否 | 否 | 编译时需要，运行时不需要（如 Lombok） |
| `runtimeOnly` | 否 | 是 | 否 | 编译时不需要，运行时需要（如 JDBC 驱动） |
| `annotationProcessor` | 编译时 | 否 | 否 | 注解处理器专用（如 Lombok） |
| `testImplementation` | 测试编译 | 测试运行 | 否 | 测试代码专用依赖 |

**`implementation` vs `api` 区别示例**：

```kotlin
// bz_framework_core 使用 api 暴露
dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
}

// bz_web_app 依赖 core 后，自动获得 spring-boot-starter-web
dependencies {
    implementation(project(":bz_framework_core"))
    // 无需再次声明 spring-boot-starter-web
}
```

使用 `implementation` 时，消费者无法自动获得传递依赖，需要显式声明。这虽然稍麻烦，但带来了更好的模块隔离和更快的增量编译。

---

## 8. 新增子模块完整步骤

假设我们要新增一个 `bz_common` 模块来存放公共工具类：

### 步骤1：创建模块目录和构建脚本

```bash
mkdir -p bz_common/src/main/java/com/linan/common
mkdir -p bz_common/src/main/resources
mkdir -p bz_common/src/test/java
```

### 步骤2：编写模块构建脚本

文件路径：`backend/bz_common/build.gradle.kts`

```kotlin
plugins {
    id("java")
}

group = "com.linan"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

### 步骤3：在 settings.gradle.kts 中注册

```kotlin
include("bz_framework_core")
include("bz_web_app")
include("bz_common")  // 新增这行
```

### 步骤4：在其他模块中引用

```kotlin
// bz_web_app/build.gradle.kts
dependencies {
    implementation(project(":bz_framework_core"))
    implementation(project(":bz_common"))  // 新增依赖
}
```

### 步骤5：刷新 Gradle 项目

在 IDEA 中点击 Gradle 面板的刷新按钮，或执行：

```bash
./gradlew build
```

---

## 9. 常用 Gradle 命令

```bash
# 编译所有模块
./gradlew build

# 编译指定模块
./gradlew :bz_framework_core:build

# 清理构建产物
./gradlew clean

# 运行所有测试
./gradlew test

# 运行指定模块的测试
./gradlew :bz_framework_core:test

# 查看模块依赖树
./gradlew :bz_web_app:dependencies

# 启动 Spring Boot 应用
./gradlew :bz_web_app:bootRun

# 打包为可执行 JAR
./gradlew :bz_web_app:bootJar
```

---

## 10. 本章完成检查清单

完成本章后，请确认以下理解：

- [ ] 理解 `apply false` 的作用（声明插件但不应用）
- [ ] 理解 `allprojects` 与 `subprojects` 的区别
- [ ] 理解 BOM (`platform`) 的依赖管理机制
- [ ] 理解 `implementation`、`compileOnly`、`runtimeOnly` 的区别
- [ ] 理解 `settings.gradle.kts` 中 `include` 的含义
- [ ] 能独立新增一个子模块并配置其依赖
- [ ] 理解 Gradle Wrapper 的工作原理

---

*下一章：[02-Docker环境编排](./02-Docker环境编排.md) 的下一步是核心底座开发。请进入 [02-核心底座](../02-核心底座/01-统一返回体Result设计.md)*
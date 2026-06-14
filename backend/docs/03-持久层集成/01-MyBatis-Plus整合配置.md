# 01-MyBatis-Plus整合配置

## 1. 概述

MyBatis-Plus 是 MyBatis 的增强工具包，提供开箱即用的 CRUD 方法、分页插件、乐观锁、逻辑删除等功能。BareZen_OA 选择 MyBatis-Plus 而非 JPA，原因如下：

- **对 SQL 的完全掌控**：复杂查询可以直接写 SQL，不依赖框架生成的查询
- **学习成本低**：如果你用过 MyBatis，MyBatis-Plus 是零学习成本的升级
- **中国的生态**：文档丰富、社区活跃、中文支持完善
- **与若依框架一致**：对标项目中采用同类技术，降低迁移成本

**MyBatis-Plus vs MyBatis vs JPA 对比**：

| 特性 | MyBatis | MyBatis-Plus | JPA |
| :--- | :--- | :--- | :--- |
| 简单 CRUD | 需要手写 SQL | 自带通用 Mapper | 方法命名查询 |
| 复杂查询 | 自由写 SQL | 自由写 SQL + Lambda 查询 | JPQL / Native SQL |
| 分页 | 需要插件 | 内置分页插件 | Pageable |
| 代码生成 | 第三方 | 内置生成器 | - |
| SQL 可见性 | 完全可见 | 完全可见 | 自动生成，不可控 |

---

## 2. 依赖配置

### 2.1 添加 MyBatis-Plus 依赖

在 `bz_framework_core/build.gradle.kts` 中添加以下依赖：

```kotlin
dependencies {
    // MyBatis-Plus Spring Boot 3 启动器
    // 版本 3.5.7 是目前兼容 Spring Boot 3.2.x 的最新稳定版本
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.7")

    // MySQL 数据库驱动
    // mysql-connector-j 是 MySQL 8.0+ 的新版驱动（替代旧版 mysql-connector-java）
    // 使用 runtimeOnly 因为驱动只需要运行时加载，编译时不需要
    runtimeOnly("com.mysql:mysql-connector-j")
}
```

**说明**：
- `mybatis-plus-spring-boot3-starter`：专为 Spring Boot 3.x 设计，底层使用 Jakarta EE 10 规范。不要误用不带 `3` 后缀的版本（那是给 Spring Boot 2.x 的）。
- `mysql-connector-j`：Oracle 官方 JDBC 驱动。类路径名为 `com.mysql.cj.jdbc.Driver`。

### 2.2 启用数据源

在 `bz_web_app/src/main/resources/application.yml` 中配置数据源：

```yaml
spring:
  datasource:
    # JDBC 连接 URL
    # useUnicode=true：启用 Unicode 编码
    # characterEncoding=utf8mb4：使用 utf8mb4 字符集（支持 emoji 等 4 字节字符）
    # useSSL=false：本地开发环境禁用 SSL（生产环境应启用）
    # serverTimezone=Asia/Shanghai：设置时区（必须，否则 MySQL 8.0 可能报时区错误）
    # allowPublicKeyRetrieval=true：允许客户端获取公钥（MySQL 8.0 密码认证需要）
    url: jdbc:mysql://localhost:3306/barezen_oa?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

    # HikariCP 连接池（Spring Boot 默认连接池，性能优秀）
    hikari:
      minimum-idle: 5               # 连接池最小空闲连接数
      maximum-pool-size: 20         # 连接池最大连接数
      connection-timeout: 30000     # 获取连接超时（30秒）
      idle-timeout: 600000          # 空闲连接超时（10分钟后回收）
      max-lifetime: 1800000         # 连接最大存活时间（30分钟后重建）
```

**HikariCP 连接池说明**：

Spring Boot 2.x 起默认使用 HikariCP 作为数据库连接池（替代了 Tomcat JDBC Pool）。HikariCP 以其极致的性能和极小的资源占用成为 Java 生态中最流行的连接池。不需要额外引入依赖。

---

## 3. MyBatis-Plus 配置类

在 `bz_framework_core` 模块创建 MyBatis-Plus 的全局配置类。

### 3.1 创建 MyBatisPlusConfig.java

文件路径：`bz_framework_core/src/main/java/com/linan/core/config/MyBatisPlusConfig.java`

首先创建所需目录：

```bash
mkdir -p bz_framework_core/src/main/java/com/linan/core/config
```

```java
package com.linan.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置类
 *
 * 负责：
 * 1. 注册分页插件（PaginationInnerInterceptor）
 * 2. 指定 Mapper 接口的扫描路径
 *
 * 注意：MapperScan 注解的包路径必须与实际 Mapper 接口所在包一致。
 * 当前项目所有 Mapper 接口放置在 com.linan.**.mapper 包下。
 */
@Configuration
@MapperScan("com.linan.**.mapper")  // 扫描所有子模块的 Mapper 接口
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器注册
     *
     * MybatisPlusInterceptor 是 MyBatis-Plus 3.4+ 版本的核心拦截器容器，
     * 通过向其添加 InnerInterceptor 来启用各种特性。
     *
     * 当前添加了分页内部拦截器：
     * - PaginationInnerInterceptor：自动拦截 SQL 并追加分页语句（如 LIMIT）
     * - DbType.MYSQL：指定数据库类型为 MySQL，生成 MySQL 兼容的分页 SQL
     *
     * @return MybatisPlusInterceptor 实例，Spring 会将其注册到 MyBatis 的拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建拦截器容器
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 创建分页内部拦截器，并指定数据库类型为 MySQL
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);

        // 设置单页最大查询条数限制（防止恶意请求超大 pageSize）
        // 设为 -1 表示不限制，生产环境建议设为合理值如 500
        paginationInterceptor.setMaxLimit(-1L);

        // 溢出处理：当请求的页码超过总页数时，自动跳转到最后一页
        paginationInterceptor.setOverflow(true);

        // 将分页拦截器添加到容器中
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 后续可在此添加其他拦截器：
        // interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor()); // 乐观锁
        // interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());     // 防全表更新/删除

        return interceptor;
    }
}
```

**逐行讲解**：

- `@MapperScan("com.linan.**.mapper")`：自动扫描所有子模块中 `mapper` 包下的 Mapper 接口。`**` 通配符匹配多级目录（如 `com.linan.system.mapper`、`com.linan.common.mapper` 等）。被扫描的接口无需在每个 Mapper 上添加 `@Mapper` 注解。

- `MybatisPlusInterceptor`：MyBatis-Plus 3.4+ 的统一拦截器容器。之前的版本直接使用 `PaginationInterceptor` 已被废弃。

- `PaginationInnerInterceptor(DbType.MYSQL)`：指定数据库类型为 MySQL。MyBatis-Plus 根据此类型生成对应的分页 SQL（如 MySQL 使用 `LIMIT offset, size`，Oracle 使用 `ROWNUM`）。

- `setMaxLimit(-1L)`：设置单页最大查询条数。-1 表示不限制。生产环境建议设置为合理值（如 500），防止客户端请求 `pageSize=100000` 拖垮数据库。

- `setOverflow(true)`：当请求的页码超过实际总页数时，自动设置为最后一页。例如总页数是 5，请求第 10 页，则返回第 5 页的数据。

### 3.2 可选：SQL 执行日志配置

在 `application.yml` 中添加：

```yaml
mybatis-plus:
  configuration:
    # 控制台打印 SQL 语句（开发环境建议开启，生产环境必须关闭）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

或者使用 SLF4J 日志：

```yaml
logging:
  level:
    com.linan.**.mapper: DEBUG  # 打印 Mapper 的 SQL 日志
```

---

## 4. 自动填充处理器 (MetaObjectHandler)

在 `bz_framework_core` 模块中，我们实现了 `MetaObjectHandlerConfig`：

*   **动态用户 ID**：通过调用 `AuthInterceptor.getCurrentUserId()`，系统能自动识别当前登录用户并填入 `createBy` 和 `updateBy` 字段。
*   **时间戳管理**：利用 `LocalDateTime.now()` 自动维护记录的创建与更新时间。

> **注意**：如果操作是在未登录状态下触发的（例如系统定时任务），`createBy` 和 `updateBy` 将会被填充为 `null`。

---

## 5. 验证数据库连接

### 4.1 确保 Docker 容器运行

```bash
cd backend
make env-up

# 验证容器状态
docker ps | grep zen
```

### 4.2 启动应用验证连接

```bash
./gradlew :bz_web_app:bootRun

# 观察启动日志中是否有数据源初始化的信息
# 预期看到：
# HikariPool-1 - Starting...
# HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@xxx
# HikariPool-1 - Start completed.
```

如果数据源连接失败，启动日志会显示类似：

```
Failed to configure a DataSource: 'url' attribute is not specified
# 或
CommunicationsException: Communications link failure
```

---

## 6. 目录结构约定

为保持项目结构清晰，BareZen_OA 约定以下目录结构：

```text
bz_framework_core/
└── src/main/java/com/linan/
    ├── core/
    │   ├── common/api/          # 统一返回体、状态码
    │   ├── common/exception/    # 全局异常处理
    │   ├── security/            # JWT 工具、鉴权拦截器
    │   └── config/              # 全局配置（Web、MyBatis-Plus）
    │
    └── system/                  # 系统管理模块（后续创建）
        ├── entity/              # 实体类（对应数据库表）
        ├── mapper/              # Mapper 接口（数据访问层）
        ├── service/             # Service 接口
        │   └── impl/            # Service 实现类
        └── controller/          # Controller 控制器
```

**分层职责**：

| 层次 | 职责 | 不可做的事 |
| :--- | :--- | :--- |
| Entity | 映射数据库表结构 | 不包含业务逻辑 |
| Mapper | 数据访问，定义 CRUD 和自定义查询 | 不写业务校验 |
| Service | 业务逻辑，事务管理 | 不操作 HTTP 请求/响应 |
| Controller | 接收请求，参数校验，调用 Service | 不写业务逻辑和 SQL |

---

## 7. 后续章节预告

完成 MyBatis-Plus 的基础配置后，下一章将创建：
- `BaseEntity`：抽取所有实体类的公共字段（id、createTime、updateTime 等）
- `BaseMapper`：封装通用 CRUD 操作
- `BaseService`：封装通用业务逻辑
- `BaseController`：封装通用 HTTP 接口

这些基类是 BareZen_OA 对标若依框架中对通用 CRUD 能力的封装基础。

---

## 8. 常见问题

**Q: 启动时报 `No qualifying bean of type 'javax.sql.DataSource'`？**

A: 检查以下几点：
1. `application.yml` 中的 datasource 配置是否取消注释
2. MySQL 驱动依赖是否已添加
3. Docker 容器是否正常启动（`docker ps | grep mysql`）
4. 数据库名、用户名、密码是否正确

**Q: 启动时报 `The server time zone value '????????' is unrecognized`？**

A: MySQL 8.0 的时区问题。在 JDBC URL 中添加参数 `serverTimezone=Asia/Shanghai` 即可解决。

**Q: MyBatis-Plus 和 Spring Boot 的版本兼容性？**

A: `mybatis-plus-spring-boot3-starter:3.5.7` 兼容 Spring Boot 3.2.x 和 JDK 17+。不要使用不带 `3` 后缀的 `mybatis-plus-boot-starter`（那是 Spring Boot 2.x 版本）。

**Q: `@MapperScan` 和 在每个 Mapper 上加 `@Mapper` 有什么区别？**

A: `@MapperScan` 是批量扫描，一个注解覆盖所有 Mapper，更简洁。`@Mapper` 需要每个 Mapper 接口单独标注，容易遗漏但更显式。BareZen_OA 使用 `@MapperScan`。

---

## 9. 本章完成检查清单

- [ ] `bz_framework_core/build.gradle.kts` 已添加 MyBatis-Plus 和 MySQL 驱动依赖
- [ ] `application.yml` 中数据源配置已启用
- [ ] `MyBatisPlusConfig.java` 已创建并注册了分页插件
- [ ] `make env-up` 后容器正常运行
- [ ] `./gradlew :bz_web_app:bootRun` 启动成功，日志中看到 HikariPool 初始化

---

*下一章：[02-通用BaseEntity与BaseController](./02-通用BaseEntity与BaseController.md)*
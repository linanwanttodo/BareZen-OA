# 02-通用BaseEntity与BaseController

## 1. 概述

本章创建 BareZen_Base 的通用基类体系，这是对标若依框架中 CRUD 封装能力的核心基础。通过抽取公共字段和通用操作方法，避免在每一个实体类中重复编写相同的代码。

**本章创建的文件**：

| 文件 | 说明 |
| :--- | :--- |
| `BaseEntity.java` | 所有实体类的公共基类（id、时间戳、逻辑删除等） |
| `MetaObjectHandlerConfig.java` | 自动填充处理器（创建时间、更新时间、创建者等） |
| `BaseController.java` | Controller 通用基类（封装 CRUD 接口） |

---

## 2. 设计原理

### 2.1 为什么需要 BaseEntity？

在后台管理系统中，几乎每张表都需要以下字段：

```sql
id           BIGINT PRIMARY KEY    -- 主键
create_time  DATETIME              -- 创建时间
update_time  DATETIME              -- 更新时间
create_by    BIGINT                -- 创建者ID
update_by    BIGINT                -- 更新者ID
del_flag     TINYINT DEFAULT 0     -- 逻辑删除标记
```

如果每个实体类都重复定义这些字段，不仅代码冗余，而且字段名、注解配置容易不一致。通过 `BaseEntity` 统一抽取后，子类只需关注自己特有的业务字段。

```java
// 不需要继承 BaseEntity 之前，每个实体类都要写：
@Entity
public class SysUser {
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // ... 重复代码

// 继承 BaseEntity 之后，子类只需关注业务字段：
@Data
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String userName;
    private String password;
    // 只写业务字段，id/createTime 等从父类继承
}
```

### 2.2 雪花算法 vs 自增ID

| 特性 | 自增ID | 雪花ID |
| :--- | :--- | :--- |
| 分布式支持 | 不支持（多库时冲突） | 天然支持 |
| 生成时机 | 插入数据库后 | 应用层生成 |
| 趋势递增 | 完全递增 | 大致递增 |
| 预测性 | 易被猜测 | 不可预测（安全性更高） |
| 长度 | int (32位) | long (64位) |

### 2.3 逻辑删除 vs 物理删除

```text
物理删除：DELETE FROM sys_user WHERE id = 1
        数据从磁盘彻底删除，无法恢复

逻辑删除：UPDATE sys_user SET del_flag = 1 WHERE id = 1
        数据仍在磁盘，查询时自动过滤 del_flag = 1 的记录
```

---

## 3. 代码实现

### 3.1 文件清单（本章新建）

| 文件路径 | 说明 |
| :--- | :--- |
| `bz_framework_core/.../common/entity/BaseEntity.java` | 实体类公共基类 |
| `bz_framework_core/.../core/config/MetaObjectHandlerConfig.java` | MyBatis-Plus 字段自动填充配置 |
| `bz_framework_core/.../common/controller/BaseController.java` | Controller 通用基类 |

---

### 3.2 BaseEntity.java — 实体类公共基类

文件路径：`bz_framework_core/src/main/java/com/linan/core/common/entity/BaseEntity.java`

```java
package com.linan.core.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体类公共基类
 *
 * 所有数据库实体类都应该继承此类，统一管理公共字段。
 * 避免每个实体类重复定义 id、createTime、updateTime 等字段。
 *
 * 继承了 MyBatis-Plus 的以下特性：
 * - 主键（@TableId）：使用雪花算法自动生成 ID
 * - 创建时间（@TableField + fill）：插入时自动填充
 * - 更新时间（@TableField + fill）：更新时自动填充
 * - 逻辑删除（@TableLogic）：删除仅标记字段，不物理删除
 *
 * @author BareZen_Base Team
 * @since 1.0-SNAPSHOT
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID，使用雪花算法自动生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 创建者ID，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    @JsonIgnore
    private Long createBy;

    /** 更新者ID，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonIgnore
    private Long updateBy;

    /** 逻辑删除标记：0=未删除，1=已删除 */
    @TableLogic
    @JsonIgnore
    private Integer delFlag;
}
```

**逐行讲解**：

- `public abstract class BaseEntity`：使用 `abstract` 修饰，表示此类不能直接实例化，只能被继承。这确保了所有实体类都通过继承获得公共字段。

- `implements Serializable`：实现序列化接口。虽然不是每个实体都需要序列化（如 Redis 缓存、分布式Session），但遵循"能用即用"原则。

- `@TableId(type = IdType.ASSIGN_ID)`：声明此字段是主键，使用雪花算法生成 ID。MyBatis-Plus 的雪花算法基于 Twitter 的 Snowflake，由 41 位时间戳 + 10 位机器标识 + 12 位序列号组成。

- `@TableField(fill = FieldFill.INSERT)`：声明此字段在插入时自动填充。需要配合 MetaObjectHandler 使用。`FieldFill.INSERT_UPDATE` 表示在插入和更新时都填充。

- `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`：Jackson 序列化时的日期格式。如果不指定，LocalDateTime 默认序列化为 `[2026,5,5,14,30,0]` 数组格式，不符合前端要求。

- `@JsonIgnore`：序列化 JSON 时忽略此字段。`createBy` 和 `updateBy` 是内部使用的用户 ID，前端通常显示用户名而非 ID，因此不返回给前端。

- `@TableLogic`：逻辑删除注解。MyBatis-Plus 在检测到此注解后会：
  1. 将 DELETE 语句自动转换为 `UPDATE SET del_flag = 1`
  2. 查询时自动追加 `WHERE del_flag = 0`
  3. 更新时自动忽略已删除的记录

**使用方式**：

```java
// 子类只需继承，无需重复定义公共字段
@Data
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String userName;
    private String password;
    private String nickName;
    // ... 其他业务字段
}
```

---

### 3.3 MetaObjectHandlerConfig.java — 自动填充处理器

BaseEntity 中 `@TableField(fill = ...)` 仅声明了"需要填充"，具体的填充逻辑由 `MetaObjectHandler` 实现。

文件路径：`bz_framework_core/src/main/java/com/linan/core/config/MetaObjectHandlerConfig.java`

```java
package com.linan.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 *
 * 负责在插入和更新时自动填充 BaseEntity 中的公共字段：
 * - createTime / updateTime：时间戳
 * - createBy / updateBy：操作者ID（当前登录用户）
 *
 * 工作流程：
 * 1. MyBatis-Plus 在执行 INSERT 或 UPDATE 前调用此处理器
 * 2. 根据 @TableField(fill = ...) 的配置判断哪些字段需要填充
 * 3. 此处理器提供当前时间、当前用户 ID 等值
 *
 * 注意：createBy / updateBy 需要从当前登录用户获取。
 * 当前阶段使用固定值 1（后续登录功能实现后，从 SecurityContext 中获取）。
 */
@Slf4j
@Component
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    /**
     * 插入时的自动填充
     *
     * 当执行 INSERT 操作时，MyBatis-Plus 会调用此方法。
     * 为标记了 FieldFill.INSERT 或 FieldFill.INSERT_UPDATE 的字段填充值。
     *
     * @param metaObject 元对象，通过它设置字段值
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始自动填充插入字段...");

        // 填充创建时间
        // strictInsertFill 会检查字段是否存在以及是否配置了 fill = INSERT
        // 第一个参数 metaObject：元对象
        // 第二个参数 "createTime"：实体类中的字段名
        // 第三个参数 LocalDateTime.class：字段类型
        // 第四个参数 LocalDateTime.now()：填充的值
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());

        // 填充更新时间（插入时也需要设置更新时间）
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充创建者ID
        // TODO: 登录功能完成后，从 SecurityContext 获取当前用户ID
        Long currentUserId = getCurrentUserId();
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);

        // 填充更新者ID
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 更新时的自动填充
     *
     * 当执行 UPDATE 操作时，MyBatis-Plus 会调用此方法。
     * 为标记了 FieldFill.UPDATE 或 FieldFill.INSERT_UPDATE 的字段填充值。
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始自动填充更新字段...");

        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充更新者ID
        Long currentUserId = getCurrentUserId();
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 获取当前登录用户 ID
     *
     * 占位实现：当前返回固定值 1L。
     * 登录功能完成后，实现从 SecurityContext 或 ThreadLocal 中获取真实的用户 ID。
     *
     * @return 当前登录用户的 ID
     */
    private Long getCurrentUserId() {
        // TODO: 从 SecurityContext 获取当前登录用户
        return 1L;
    }
}
```

**逐行讲解**：

- `implements MetaObjectHandler`：实现 MyBatis-Plus 提供的元对象处理器接口。MyBatis-Plus 会在执行 insert/update 操作前自动调用此处理器的对应方法。

- `@Component`：Spring 容器会扫描并注册此 Bean。MyBatis-Plus 自动配置时会检测到它并启用自动填充。

- `strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now())`：
  - `strictInsertFill`：严格插入填充。它会先检查实体中是否存在该字段，以及是否配置了 `fill = FieldFill.INSERT`。如果字段不存在或配置不匹配，则跳过填充。
  - 第一参数：metaObject，MyBatis 反射包装对象
  - 第二参数："createTime"，实体中字段名（Java 驼峰命名）
  - 第三参数：字段类型
  - 第四参数：要填充的值

- `getCurrentUserId()`：当前是占位实现（返回固定值 1）。后续实现用户登录功能后，需要改为从 `SecurityContextHolder` 中获取当前登录用户的 ID。

---

### 3.4 BaseController.java — Controller 通用基类

虽然 MyBatis-Plus 提供了 `IService` 接口封装了 `save`/`removeById`/`updateById`/`getById` 等方法，但 Controller 层的返回格式和参数校验逻辑仍然需要统一。`BaseController` 提供通用的 Controller 方法骨架。

文件路径：`bz_framework_core/src/main/java/com/linan/core/common/controller/BaseController.java`

```java
package com.linan.core.common.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.core.common.api.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller 通用基类
 *
 * 提供通用的 CRUD 接口实现。子类 Controller 继承此基类后，
 * 自动获得以下接口，无需重复编写：
 * - GET    /list       分页查询
 * - GET    /{id}       根据ID查询
 * - POST   /           新增
 * - PUT    /           更新
 * - DELETE /{id}       删除
 *
 * 使用方式：
 * <pre>
 * @RestController
 * @RequestMapping("/api/system/v1/users")
 * public class UserController extends BaseController<User, UserService> {
 *     // 自动继承增删改查接口，只需添加自定义业务接口
 * }
 * </pre>
 *
 * @param <T> 实体类型（如 User）
 * @param <S> Service 类型（如 UserService，需继承 IService<T>）
 */
public abstract class BaseController<T, S extends IService<T>> {

    /**
     * 子类通过此方法提供自己的 Service 实例。
     * 需要子类实现，通常直接返回 @Autowired 注入的 service。
     */
    protected abstract S getService();

    /**
     * 分页查询
     *
     * 请求：GET /{controller-path}/list?pageNum=1&pageSize=10
     * 响应：{"code":200,"msg":"操作成功","data":{"total":100,"list":[...]}}
     */
    @GetMapping("/list")
    public Result<IPage<T>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<T> page = new Page<>(pageNum, pageSize);
        IPage<T> result = getService().page(page);
        return Result.success(result);
    }

    /**
     * 根据ID查询
     *
     * 请求：GET /{controller-path}/{id}
     * 响应：{"code":200,"msg":"操作成功","data":{...}}
     */
    @GetMapping("/{id}")
    public Result<T> getById(@PathVariable Long id) {
        T entity = getService().getById(id);
        if (entity == null) {
            return Result.error("数据不存在");
        }
        return Result.success(entity);
    }

    /**
     * 新增
     *
     * 请求：POST /{controller-path}  Body: {...}
     * 响应：{"code":200,"msg":"操作成功","data":null}
     */
    @PostMapping
    public Result<Void> save(@RequestBody T entity) {
        boolean ok = getService().save(entity);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    /**
     * 更新
     *
     * 请求：PUT /{controller-path}  Body: {...}
     * 响应：{"code":200,"msg":"操作成功","data":null}
     */
    @PutMapping
    public Result<Void> update(@RequestBody T entity) {
        boolean ok = getService().updateById(entity);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    /**
     * 删除
     *
     * 请求：DELETE /{controller-path}/{id}
     * 响应：{"code":200,"msg":"操作成功","data":null}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = getService().removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}
```

**逐行讲解**：

- `public abstract class BaseController<T, S extends IService<T>>`：两个泛型参数：
  - T：实体类型，如 User、Role
  - S extends IService<T>：Service 类型，必须继承 MyBatis-Plus 的 IService<T>。这样在基类中可以直接调用 `getService().page()` 等通用方法。

- `protected abstract S getService()`：抽象方法，子类必须实现。典型实现方式：
  ```java
  @Autowired
  private UserService userService;
  @Override
  protected UserService getService() { return userService; }
  ```

- `@RequestParam(defaultValue = "1")`：为 pageNum 设置默认值。当前端不传参数时，自动使用第一页。

- `IPage<T>`：MyBatis-Plus 的分页结果接口，包含 `getRecords()`（当前页数据）、`getTotal()`（总记录数）等方法。

---

## 4. 接口文档（附在手册中供前端对接）

BaseController 提供以下通用接口：

| 接口 | 方法 | URL 示例 | 说明 |
| :--- | :---: | :--- | :--- |
| 分页查询 | GET | /api/system/v1/users/list?pageNum=1&pageSize=10 | 查询分页列表 |
| 根据ID查询 | GET | /api/system/v1/users/1 | 查询单个记录 |
| 新增 | POST | /api/system/v1/users | Body 传入 JSON |
| 更新 | PUT | /api/system/v1/users | Body 传入 JSON（含 id） |
| 删除 | DELETE | /api/system/v1/users/1 | 逻辑删除 |

---

## 5. 测试验证

完成本章代码后，编译验证：

```bash
cd backend
./gradlew :bz_framework_core:build
```

预期输出：`BUILD SUCCESSFUL`

---

## 6. 常见问题

**Q: 为什么 BaseEntity 使用 abstract 而不是普通 class？**

A: `abstract` 表示此类不能直接实例化，语义上更清晰——"这是一个基类，请继承使用"。如果使用普通 class，理论上可以 `new BaseEntity()`，但这没有意义。

**Q: 雪花 ID 一定能保证全局唯一吗？**

A: 在单机部署下，雪花算法由"时间戳 + 机器ID + 序列号"组成，保证了唯一性。在多机部署时，需要配置不同的 `workerId` 和 `datacenterId`（MyBatis-Plus 默认自动检测 MAC 地址）。

**Q: @JsonIgnore 后怎么获取 createBy？**

A: 如果某些场景需要返回创建者信息（如审计日志），可以在具体的 Controller 中通过 `createBy` 关联查询用户名后，以自定义字段返回，而不是直接返回 `createBy` 字段。

---

## 7. 本章完成检查清单

- [ ] `BaseEntity.java` 已创建，包含 id/createTime/updateTime/createBy/updateBy/delFlag
- [ ] `MetaObjectHandlerConfig.java` 已创建，实现了 insertFill 和 updateFill
- [ ] `BaseController.java` 已创建，提供 list/getById/save/update/delete 通用接口
- [ ] `./gradlew :bz_framework_core:build` 编译通过

---

*下一章：[03-分页查询统一封装](./03-分页查询统一封装.md)*
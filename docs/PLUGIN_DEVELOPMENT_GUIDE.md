# BareZen_OA 插件开发详细指南

> **现状说明（2026-11 核对）**：本指南描述的是目标开发流程。当前实际状态：
> - 插件主类（extends AbstractPlugin + @Component）会被 `PluginAutoConfiguration` 在启动时发现并注册到 PluginManager，这条链路可用
> - 但 `plugins/` 目录下的清单 JSON 暂无代码读取，"步骤4：创建插件清单" 目前只起文档作用；前端路由与权限实际通过 Flyway 菜单脚本（sys_menu 表）注册
> - 迁移脚本统一放 `backend/bz_web_app/src/main/resources/db/migration/`（bz_framework_core 下的 migration 目录是空的），版本号顺延现有最大号（当前到 V50+）
> - 文中 `V37__customer_plugin.sql`、菜单 ID 10031+ 等均为示例编号，实际以迁移目录里现有最大版本为准

## 目录
1. [插件开发概述](#插件开发概述)
2. [快速开始](#快速开始)
3. [详细开发步骤](#详细开发步骤)
4. [插件接入](#插件接入)
5. [插件删除](#插件删除)
6. [插件管理](#插件管理)
7. [常见问题](#常见问题)
8. [最佳实践](#最佳实践)

---

## 插件开发概述

### 什么是插件？

插件是BareZen_OA系统中的独立功能模块，就像乐高积木一样，可以按需添加、移除，不会影响核心系统的稳定性。

### 插件的优势

- **独立开发**：每个插件独立开发，互不影响
- **按需启用**：根据业务需求启用相关插件
- **易于维护**：插件职责单一，便于维护
- **快速迭代**：插件可以独立发布和升级
- **团队协作**：不同团队可以并行开发不同插件

### 插件适用场景

- * **业务模块**：工资管理、考勤管理、报销管理等
- * **功能扩展**：第三方集成、自定义报表等
- * **行业定制**：特定行业的定制功能
- * **试点功能**：新功能的灰度发布

---

## 快速开始

### 5分钟创建第一个插件

让我们创建一个简单的"客户管理"插件作为示例。

#### 步骤1：创建插件目录

```bash
cd /home/lin/All_projects/Javaproject/BareZen_OA/backend/bz_framework_core/src/main/java/com/linan/plugin
mkdir -p customer
cd customer
```

#### 步骤2：创建插件主类

创建 `CustomerPlugin.java`：

```java
package com.linan.plugin.customer;

import com.linan.core.plugin.AbstractPlugin;
import com.linan.core.plugin.PluginException;
import com.linan.core.plugin.PluginStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerPlugin extends AbstractPlugin {

    @Override
    public String getPluginId() {
        return "com.linan.plugin.customer";
    }

    @Override
    public String getPluginName() {
        return "客户管理模块";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "提供客户信息管理、客户分类、客户跟进等功能";
    }

    @Override
    public String getAuthor() {
        return "BareZen Team";
    }

    @Override
    public void initialize() throws PluginException {
        log.info("初始化客户管理插件...");
        // 初始化逻辑
        super.initialize();
    }

    @Override
    public void start() throws PluginException {
        log.info("启动客户管理插件...");
        // 启动逻辑
        super.start();
    }

    @Override
    public void stop() throws PluginException {
        log.info("停止客户管理插件...");
        // 停止逻辑
        super.stop();
    }
}
```

#### 步骤3：创建数据库迁移脚本

创建 `V37__customer_plugin.sql`：

```sql
-- 客户信息表
CREATE TABLE IF NOT EXISTS customer_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    customer_name VARCHAR(100) NOT NULL COMMENT '客户名称',
    customer_code VARCHAR(50) COMMENT '客户编码',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '地址',
    status VARCHAR(1) DEFAULT '0' COMMENT '状态: 0=正常, 1=停用',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag INT DEFAULT 0 COMMENT '删除标记',
    UNIQUE KEY uk_customer_code (customer_code),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息表';

-- 插入客户管理菜单
INSERT INTO sys_menu (id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(10031, '客户管理', 0, 11, 'customer', NULL, 1, 0, 'M', '0', '0', NULL, 'user', 1, NOW(), NULL, NULL, '客户管理模块'),
(10032, '客户列表', 10031, 1, 'list', 'customer/list/index', 1, 0, 'C', '0', '0', 'customer:list', 'list', 1, NOW(), NULL, NULL, '客户列表管理');

-- 插入客户管理按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(10033, '客户查询', 10032, 1, '', '', 1, 0, 'F', '0', '0', 'customer:list', '#', 1, NOW(), NULL, NULL, ''),
(10034, '客户新增', 10032, 2, '', '', 1, 0, 'F', '0', '0', 'customer:add', '#', 1, NOW(), NULL, NULL, ''),
(10035, '客户修改', 10032, 3, '', '', 1, 0, 'F', '0', '0', 'customer:edit', '#', 1, NOW(), NULL, NULL, ''),
(10036, '客户删除', 10032, 4, '', '', 1, 0, 'F', '0', '0', 'customer:delete', '#', 1, NOW(), NULL, NULL, '');
```

#### 步骤4：创建插件清单

创建 `customer-plugin.json`：

```json
{
  "pluginId": "com.linan.plugin.customer",
  "pluginName": "客户管理模块",
  "version": "1.0.0",
  "description": "提供客户信息管理、客户分类、客户跟进等功能",
  "author": "BareZen Team",
  "dependencies": [],
  "configuration": {
    "customer.auto.assign.enabled": false,
    "customer.max.followers": 5
  },
  "controllers": [
    "com.linan.plugin.customer.controller.CustomerController"
  ],
  "services": [
    "com.linan.plugin.customer.service.ICustomerService"
  ],
  "entities": [
    "com.linan.plugin.customer.entity.CustomerInfo"
  ],
  "migrations": [
    "V37__customer_plugin.sql"
  ],
  "frontendRoutes": [
    {
      "path": "/customer",
      "component": "Layout",
      "redirect": "/customer/list",
      "children": [
        {
          "path": "list",
          "component": "customer/list/index",
          "name": "CustomerList",
          "meta": {
            "title": "客户列表",
            "icon": "list",
            "roles": ["customer:list"]
          }
        }
      ]
    }
  ],
  "permissions": [
    "customer:list",
    "customer:add",
    "customer:edit",
    "customer:delete"
  ]
}
```

#### 步骤5：创建业务代码

按照标准MVC结构创建：

```
customer/
├── entity/
│   └── CustomerInfo.java
├── mapper/
│   └── CustomerInfoMapper.java
├── service/
│   ├── ICustomerService.java
│   └── impl/
│       └── CustomerServiceImpl.java
└── controller/
    └── CustomerController.java
```

#### 步骤6：启动应用

```bash
cd /home/lin/All_projects/Javaproject/BareZen_OA/backend
./gradlew :bz_web_app:bootRun
```

应用启动后会自动加载和启动你的插件！

---

## 详细开发步骤

### 1. 插件目录结构

标准的插件目录结构：

```
com.linan.plugin.{plugin_name}/
├── {PluginName}Plugin.java          # 插件主类（必须）
├── entity/                          # 实体类
│   ├── Entity1.java
│   └── Entity2.java
├── mapper/                          # 数据访问层
│   ├── Entity1Mapper.java
│   └── Entity2Mapper.java
├── service/                         # 服务层
│   ├── IEntity1Service.java
│   ├── IEntity2Service.java
│   └── impl/
│       ├── Entity1ServiceImpl.java
│       └── Entity2ServiceImpl.java
├── controller/                      # 控制器层
│   ├── Entity1Controller.java
│   └── Entity2Controller.java
└── dto/                            # 数据传输对象（可选）
    ├── Entity1DTO.java
    └── Entity2DTO.java
```

### 2. 插件主类开发

#### 2.1 继承AbstractPlugin

```java
@Component  // 必须添加@Component注解，让Spring管理
public class YourPlugin extends AbstractPlugin {

    @Override
    public String getPluginId() {
        // 插件ID，必须全局唯一
        // 格式：com.linan.plugin.{plugin_name}
        return "com.linan.plugin.your_plugin";
    }

    @Override
    public String getPluginName() {
        // 插件名称，显示给用户
        return "你的插件名称";
    }

    @Override
    public String getVersion() {
        // 版本号，遵循语义化版本
        // 格式：MAJOR.MINOR.PATCH
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        // 插件描述
        return "插件功能描述";
    }

    @Override
    public String getAuthor() {
        // 插件作者
        return "你的名字或团队";
    }

    @Override
    public void initialize() throws PluginException {
        // 插件初始化逻辑
        // 在插件加载时调用
        super.initialize();
    }

    @Override
    public void start() throws PluginException {
        // 插件启动逻辑
        // 在应用启动完成后调用
        super.start();
    }

    @Override
    public void stop() throws PluginException {
        // 插件停止逻辑
        // 在应用关闭时调用
        super.stop();
    }
}
```

#### 2.2 插件生命周期详解

```
应用启动
    ↓
发现插件（通过@Component注解）
    ↓
调用 initialize() - 初始化插件
    ↓
检查依赖关系
    ↓
执行数据库迁移
    ↓
注册权限和菜单
    ↓
调用 start() - 启动插件
    ↓
插件可用
    ↓
应用关闭
    ↓
调用 stop() - 停止插件
    ↓
清理资源
```

### 3. 实体类开发

#### 3.1 创建实体类

```java
package com.linan.plugin.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_info")
@Schema(description = "客户信息")
public class CustomerInfo {

    @TableId(type = IdType.AUTO)
    @Schema(description = "客户ID")
    private Long id;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户编码")
    private String customerCode;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "状态: 0=正常, 1=停用")
    private String status;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "更新人")
    private Long updateBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "删除标记")
    private Integer delFlag;
}
```

#### 3.2 实体类规范

- * 使用 `@TableName` 注解指定表名
- * 使用 `@TableId` 注解标注主键
- * 使用 `@Schema` 注解添加Swagger文档
- * 使用 Lombok 的 `@Data` 注解简化代码
- * 字段命名使用驼峰命名法
- * 包含标准的审计字段（createBy, updateBy, createTime, updateTime, delFlag）

### 4. Mapper开发

#### 4.1 创建Mapper接口

```java
package com.linan.plugin.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.linan.plugin.customer.entity.CustomerInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
    // 继承BaseMapper即可获得基本的CRUD方法
    // 可以自定义SQL方法
}
```

#### 4.2 Mapper规范

- * 使用 `@Mapper` 注解标注
- * 继承 `BaseMapper<Entity>` 获得基本CRUD
- * 复杂SQL可以在XML文件中编写
- * 自定义方法需要在对应的XML中实现

### 5. Service开发

#### 5.1 创建Service接口

```java
package com.linan.plugin.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.plugin.customer.entity.CustomerInfo;

import java.util.List;

public interface ICustomerService extends IService<CustomerInfo> {

    /**
     * 根据客户编码查询
     */
    CustomerInfo getByCustomerCode(String customerCode);

    /**
     * 批量启用客户
     */
    boolean batchEnable(List<Long> ids);

    /**
     * 批量停用客户
     */
    boolean batchDisable(List<Long> ids);
}
```

#### 5.2 创建Service实现

```java
package com.linan.plugin.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.core.common.exception.BusinessException;
import com.linan.plugin.customer.entity.CustomerInfo;
import com.linan.plugin.customer.mapper.CustomerInfoMapper;
import com.linan.plugin.customer.service.ICustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo>
        implements ICustomerService {

    @Override
    public CustomerInfo getByCustomerCode(String customerCode) {
        return getOne(new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getCustomerCode, customerCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchEnable(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("客户ID列表不能为空");
        }

        return update(new LambdaQueryWrapper<CustomerInfo>()
                .in(CustomerInfo::getId, ids)
                .set(CustomerInfo::getStatus, "0"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDisable(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("客户ID列表不能为空");
        }

        return update(new LambdaQueryWrapper<CustomerInfo>()
                .in(CustomerInfo::getId, ids)
                .set(CustomerInfo::getStatus, "1"));
    }
}
```

#### 5.3 Service规范

- * 接口继承 `IService<Entity>`
- * 实现类继承 `ServiceImpl<Mapper, Entity>`
- * 使用 `@Service` 注解标注
- * 使用 `@RequiredArgsConstructor` 注入依赖
- * 使用 `@Transactional` 标注事务方法
- * 业务逻辑在Service层实现

### 6. Controller开发

#### 6.1 创建Controller

```java
package com.linan.plugin.customer.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.plugin.customer.entity.CustomerInfo;
import com.linan.plugin.customer.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "客户管理", description = "客户管理接口")
@RestController
@RequestMapping("/api/customer/v1")
@RequiredArgsConstructor
public class CustomerController {

    private final ICustomerService customerService;

    @Operation(summary = "查询客户列表")
    @RequirePermission("customer:list")
    @GetMapping
    public Result<List<CustomerInfo>> list() {
        return Result.success(customerService.list());
    }

    @Operation(summary = "根据ID查询客户")
    @RequirePermission("customer:list")
    @GetMapping("/{id}")
    public Result<CustomerInfo> getById(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id) {
        return Result.success(customerService.getById(id));
    }

    @Operation(summary = "新增客户")
    @RequirePermission("customer:add")
    @PostMapping
    public Result<Void> save(@RequestBody CustomerInfo customer) {
        boolean ok = customerService.save(customer);
        return ok ? Result.success(null) : Result.error("新增失败");
    }

    @Operation(summary = "更新客户")
    @RequirePermission("customer:edit")
    @PutMapping
    public Result<Void> update(@RequestBody CustomerInfo customer) {
        boolean ok = customerService.updateById(customer);
        return ok ? Result.success(null) : Result.error("更新失败");
    }

    @Operation(summary = "删除客户")
    @RequirePermission("customer:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "客户ID", required = true)
            @PathVariable Long id) {
        boolean ok = customerService.removeById(id);
        return ok ? Result.success(null) : Result.error("删除失败");
    }
}
```

#### 6.2 Controller规范

- * 使用 `@Tag` 注解添加模块描述
- * 使用 `@Operation` 注解添加接口描述
- * 使用 `@Parameter` 注解添加参数描述
- * 使用 `@RequirePermission` 注解添加权限控制
- * 返回统一的 `Result` 格式
- * Controller只处理HTTP请求，业务逻辑在Service

### 7. 数据库迁移

#### 7.1 创建迁移脚本

文件命名规则：`V{版本号}__{插件名}.sql`

```sql
-- V37__customer_plugin.sql

-- 创建表
CREATE TABLE IF NOT EXISTS customer_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(100) NOT NULL,
    -- 其他字段...
);

-- 插入菜单
INSERT INTO sys_menu (id, menu_name, parent_id, ...) VALUES
(10031, '客户管理', 0, 11, 'customer', NULL, 1, 0, 'M', '0', '0', NULL, 'user', 1, NOW(), NULL, NULL, '客户管理');

-- 插入权限
INSERT INTO sys_menu (id, menu_name, parent_id, ...) VALUES
(10032, '客户查询', 10031, 1, '', '', 1, 0, 'F', '0', '0', 'customer:list', '#', 1, NOW(), NULL, NULL, '');
```

#### 7.2 迁移脚本规范

- * 版本号递增（参考现有版本号）
- * 包含建表语句
- * 包含菜单插入语句
- * 包含权限插入语句
- * 使用 `IF NOT EXISTS` 避免重复创建
- * 菜单ID >= 10000（1-21已被占用）

### 8. 插件清单

#### 8.1 创建清单文件

创建 `plugins/{plugin_name}-plugin.json`：

```json
{
  "pluginId": "com.linan.plugin.customer",
  "pluginName": "客户管理模块",
  "version": "1.0.0",
  "description": "提供客户信息管理功能",
  "author": "BareZen Team",
  "dependencies": [],
  "configuration": {},
  "controllers": ["com.linan.plugin.customer.controller.CustomerController"],
  "services": ["com.linan.plugin.customer.service.ICustomerService"],
  "entities": ["com.linan.plugin.customer.entity.CustomerInfo"],
  "migrations": ["V37__customer_plugin.sql"],
  "frontendRoutes": [],
  "permissions": ["customer:list", "customer:add", "customer:edit", "customer:delete"]
}
```

#### 8.2 清单文件规范

- * pluginId 必须与插件主类一致
- * version 遵循语义化版本
- * 列出所有Controller、Service、Entity的完整类名
- * 列出所有数据库迁移脚本
- * 列出所有权限点

---

## 插件接入

### 如何接入现有插件

#### 方法1：从代码仓库克隆

```bash
# 克隆插件代码
git clone <plugin-repo-url>
cp -r plugin-name backend/bz_framework_core/src/main/java/com/linan/plugin/
```

#### 方法2：复制插件文件

```bash
# 复制插件目录
cp -r /path/to/plugin backend/bz_framework_core/src/main/java/com/linan/plugin/

# 复制数据库迁移脚本
cp /path/to/migration.sql backend/bz_framework_core/src/main/resources/db/migration/

# 复制插件清单
cp /path/to/plugin.json backend/bz_framework_core/src/main/resources/plugins/
```

#### 方法3：通过依赖接入（未来支持）

```gradle
dependencies {
    implementation 'com.linan.plugins:customer-plugin:1.0.0'
}
```

### 接入后验证

1. **检查插件是否加载**

```bash
# 启动应用
./gradlew :bz_web_app:bootRun

# 查看日志，应该看到：
# INFO  - 插件加载成功: com.linan.plugin.customer - 客户管理模块
# INFO  - 插件启动成功: com.linan.plugin.customer
```

2. **通过API验证**

```bash
# 查询所有插件
curl http://localhost:8080/api/system/v1/plugins

# 查询特定插件
curl http://localhost:8080/api/system/v1/plugins/com.linan.plugin.customer
```

3. **检查数据库**

```sql
-- 检查表是否创建
SHOW TABLES LIKE 'customer%';

-- 检查菜单是否插入
SELECT * FROM sys_menu WHERE menu_name = '客户管理';
```

---

## 插件删除

### 如何删除插件

#### 步骤1：停止插件

```bash
# 通过API停止
curl -X POST http://localhost:8080/api/system/v1/plugins/com.linan.plugin.customer/stop
```

#### 步骤2：删除代码文件

```bash
# 删除插件目录
rm -rf backend/bz_framework_core/src/main/java/com/linan/plugin/customer

# 删除数据库迁移脚本
rm backend/bz_framework_core/src/main/resources/db/migration/V37__customer_plugin.sql

# 删除插件清单
rm backend/bz_framework_core/src/main/resources/plugins/customer-plugin.json
```

#### 步骤3：清理数据库数据

```sql
-- 删除菜单
DELETE FROM sys_menu WHERE menu_name = '客户管理';

-- 删除权限
DELETE FROM sys_menu WHERE menu_name LIKE '客户%';

-- 删除表（可选，保留数据则不删除）
DROP TABLE IF EXISTS customer_info;
```

#### 步骤4：重启应用

```bash
# 重启应用
./gradlew :bz_web_app:bootRun
```

### 数据清理注意事项

**重要提示**：
- 删除插件前先备份数据库
- 评估数据是否需要保留
- 考虑数据迁移方案
- 通知相关用户插件将被移除

---

## 插件管理

### 插件状态管理

#### 查看插件状态

```bash
# 查看所有插件
curl http://localhost:8080/api/system/v1/plugins

# 查看已启用的插件
curl http://localhost:8080/api/system/v1/plugins/enabled
```

#### 启动插件

```bash
curl -X POST http://localhost:8080/api/system/v1/plugins/{pluginId}/start
```

#### 停止插件

```bash
curl -X POST http://localhost:8080/api/system/v1/plugins/{pluginId}/stop
```

#### 卸载插件

```bash
curl -X DELETE http://localhost:8080/api/system/v1/plugins/{pluginId}
```

### 插件依赖管理

#### 声明依赖

在插件清单中声明依赖：

```json
{
  "dependencies": [
    "com.linan.plugin.hr",
    "com.linan.plugin.system"
  ]
}
```

#### 依赖检查

插件管理器会自动检查：
- 依赖插件是否存在
- 依赖插件是否已加载
- 依赖插件版本是否兼容

### 插件配置管理

#### 配置项声明

在插件清单中声明配置项：

```json
{
  "configuration": {
    "your.feature.enabled": true,
    "your.feature.limit": 100
  }
}
```

#### 配置项使用

在插件代码中使用配置：

```java
@Value("${your.feature.enabled}")
private boolean featureEnabled;
```

---

## 常见问题

### Q1: 插件加载失败怎么办？

**A**: 检查以下几点：
1. 插件主类是否添加了 `@Component` 注解
2. 插件ID是否全局唯一
3. 依赖插件是否已加载
4. 数据库迁移脚本是否有语法错误
5. 查看日志中的详细错误信息

### Q2: 插件启动失败怎么办？

**A**: 检查以下几点：
1. 插件的 `start()` 方法是否有异常
2. Service、Controller是否正确注入
3. 权限和菜单是否正确注册
4. 查看日志中的详细错误信息

### Q3: 如何调试插件？

**A**: 调试步骤：
1. 在插件代码中添加日志
2. 使用IDEA的断点调试功能
3. 查看 `PluginManager` 的日志输出
4. 使用插件管理API查询插件状态

### Q4: 插件间如何通信？

**A**: 插件间通信方式：
1. 通过Service调用（推荐）
2. 通过事件机制（Spring Events）
3. 通过共享接口（定义公共接口）

### Q5: 插件如何访问核心系统功能？

**A**: 访问方式：
1. 注入核心系统的Service
2. 使用核心系统的工具类
3. 调用核心系统的API

### Q6: 插件版本如何升级？

**A**: 升级步骤：
1. 创建新的数据库迁移脚本
2. 更新插件版本号
3. 更新插件清单
4. 测试升级过程
5. 部署新版本

### Q7: 如何回滚插件？

**A**: 回滚步骤：
1. 停止插件
2. 回滚数据库迁移
3. 恢复旧版本代码
4. 重启应用

### Q8: 插件性能如何优化？

**A**: 优化建议：
1. 使用缓存减少数据库查询
2. 异步处理耗时操作
3. 批量操作代替循环操作
4. 合理使用索引

---

## 最佳实践

### 命名规范

#### 插件命名
- 插件ID：`com.linan.plugin.{模块名}`
- 插件类名：`{模块名}Plugin`
- 包名：`com.linan.plugin.{模块名}`

#### 业务命名
- Entity：`{业务名}Info`
- Mapper：`{业务名}Mapper`
- Service：`I{业务名}Service`
- Controller：`{业务名}Controller`

### 版本管理

#### 语义化版本
- `MAJOR.MINOR.PATCH`
- MAJOR：不兼容的API修改
- MINOR：向下兼容的功能性新增
- PATCH：向下兼容的问题修正

#### 数据库迁移
- 每个版本对应一个迁移脚本
- 版本号递增
- 迁移脚本不可修改

### 错误处理

#### 使用PluginException
```java
throw new PluginException(pluginId, "插件加载失败: " + message);
```

#### 使用BusinessException
```java
throw new BusinessException("业务逻辑错误");
```

### 资源管理

#### 正确释放资源
```java
@Override
public void stop() throws PluginException {
    // 停止定时任务
    // 清理缓存
    // 关闭连接
    // 释放资源
    super.stop();
}
```

### 测试

#### 单元测试
```java
@SpringBootTest
class CustomerPluginTest {
    @Test
    void testPluginInitialize() {
        // 测试插件初始化
    }
}
```

#### 集成测试
```java
@SpringBootTest
class CustomerIntegrationTest {
    @Test
    void testCustomerLifecycle() {
        // 测试完整的业务流程
    }
}
```

### 文档

#### 注释规范
```java
/**
 * 客户管理插件
 *
 * 功能：
 * - 客户信息管理
 * - 客户分类管理
 * - 客户跟进记录
 *
 * @author 作者名
 * @version 1.0.0
 */
public class CustomerPlugin extends AbstractPlugin {
    // ...
}
```

### 安全

#### 权限控制
```java
@RequirePermission("customer:add")
@PostMapping
public Result<Void> save(@RequestBody CustomerInfo customer) {
    // ...
}
```

#### 数据验证
```java
@NotBlank(message = "客户名称不能为空")
private String customerName;
```

---

## 总结

通过本指南，你应该能够：

- * 理解插件化架构的设计理念
- * 掌握插件开发的完整流程
- * 能够独立开发新的业务插件
- * 理解插件的接入和删除流程
- * 掌握插件管理的基本操作
- * 遵循插件开发的最佳实践

BareZen_OA的插件化架构让系统变得像乐高积木一样灵活，你可以：
- 按需添加功能模块
- 独立开发和测试
- 快速迭代和升级
- 灵活组合和配置

开始构建你的第一个插件吧！
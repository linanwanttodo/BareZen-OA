# BareZen_OA 插件化架构文档

> **现状说明（2026-11-24 更新）**：下列机制已真实生效，不再只是状态枚举切换：
> - **清单已接入**：启动时 `PluginManifestLoader` 扫描 `classpath:plugins/*.json`，解析并注册 `PluginManifest`，同时在门控表中登记初始状态（LOADED，关闭）。`GET /api/system/v1/plugins/{id}` 返回清单内容 + 运行状态 + loaded 标志
> - **启停真实生效**：`PluginGateAspect` 按包约定拦截 `com.linan.plugin.{module}.` 包下标注 `@RestController` 的接口（pluginId = `com.linan.plugin.{module}`）。插件非 ACTIVE（LOADED/DISABLED/FAILED/ERROR/未注册）时返回统一错误：HTTP 503 / Result code=503（文案按状态区分：「插件已停用」「插件未启动」等）。`PluginManager.start/stop` 同步更新门控状态。验收口径：管理端停用 salary 后，`/api/salary/v1/**` 立即拒绝服务
> - **卸载真实生效**：unload 从管理器移除 Plugin 实例与清单，但门控条目保留且保持关闭，该插件的 HTTP 接口继续拒绝服务。已知边界：class 仍在 classpath，属静态卸载而非类卸载
> - **启动自检**：清单声明的 controller 类在 classpath 不存在时打 WARN
> - 仍未实现（边界）：运行时类热插拔/动态加载；`SalaryPlugin` 生命周期回调仍是日志空壳（业务 Controller/Service 由 Spring 组件扫描常驻注册，不随插件停用销毁——门控在入口处拦截）；插件 Service 层被本插件 HTTP 接口以外的代码直连时不受门控（如 bz_web_app 的 `AppSalaryController` /api/app/v1/salary 直连 salary Service）；前端路由实际来自数据库菜单表（RBAC），不是清单里的 frontendRoutes；hr/asset/kb/office 等业务域位于 `com.linan.*` 直属包，未接入插件体系
> - 迁移脚本实际位置：`backend/bz_web_app/src/main/resources/db/migration/`（工资插件为 V47__salary_plugin.sql，菜单授权见 V50）

## 插件化架构概述

BareZen_OA采用插件化架构设计，支持像乐高积木一样按需添加功能模块。每个业务模块都是独立的插件，可以：

- **独立开发**：每个插件独立开发，互不影响
- **按需加载**：根据需要启用或禁用插件
- **热插拔**：支持运行时动态加载和卸载插件
- **版本管理**：每个插件独立版本，支持平滑升级
- **依赖管理**：支持插件间的依赖关系管理

## 插件目录结构

```
backend/bz_framework_core/src/main/java/com/linan/plugin/
├── salary/                          # 工资管理插件
│   ├── SalaryPlugin.java           # 插件主类
│   ├── entity/                     # 实体类
│   │   └── SalaryStructure.java
│   ├── mapper/                     # 数据访问层
│   │   └── SalaryStructureMapper.java
│   ├── service/                    # 服务层
│   │   ├── ISalaryStructureService.java
│   │   └── impl/
│   │       └── SalaryStructureServiceImpl.java
│   └── controller/                 # 控制器层
│       └── SalaryStructureController.java
└── attendance/                     # 考勤管理插件（示例）
    └── ...
```

## 插件规范

### 1. 插件接口

所有插件必须实现 `Plugin` 接口：

```java
public interface Plugin {
    String getPluginId();           // 插件ID（全局唯一）
    String getPluginName();         // 插件名称
    String getVersion();            // 版本号
    String getDescription();        // 描述
    String getAuthor();            // 作者
    List<String> getDependencies(); // 依赖列表
    void initialize();             // 初始化
    void start();                  // 启动
    void stop();                   // 停止
    PluginStatus getStatus();      // 获取状态
}
```

### 2. 插件清单文件

每个插件需要提供 `plugin.json` 清单文件：

```json
{
  "pluginId": "com.linan.plugin.salary",
  "pluginName": "工资管理模块",
  "version": "1.0.0",
  "description": "工资管理功能",
  "author": "BareZen Team",
  "dependencies": [],
  "controllers": ["..."],
  "services": ["..."],
  "entities": ["..."],
  "migrations": ["..."],
  "frontendRoutes": [...],
  "permissions": [...]
}
```

### 3. 数据库迁移

每个插件的数据库变更使用独立的Flyway脚本：

```
V36__salary_plugin.sql  # 版本号36的工资插件迁移脚本
```

## 如何创建新插件

### 步骤1：创建插件目录

```bash
mkdir -p backend/bz_framework_core/src/main/java/com/linan/plugin/your_plugin
```

### 步骤2：实现插件类

```java
@Component
public class YourPlugin extends AbstractPlugin {
    @Override
    public String getPluginId() {
        return "com.linan.plugin.your_plugin";
    }

    @Override
    public String getPluginName() {
        return "你的插件名称";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize() throws PluginException {
        // 初始化逻辑
    }

    @Override
    public void start() throws PluginException {
        // 启动逻辑
    }

    @Override
    public void stop() throws PluginException {
        // 停止逻辑
    }
}
```

### 步骤3：创建业务代码

按照标准MVC结构创建：
- Entity（实体类）
- Mapper（数据访问）
- Service（业务逻辑）
- Controller（API接口）

### 步骤4：数据库迁移

创建Flyway迁移脚本：
```
V{版本号}__{插件名}.sql
```

### 步骤5：创建插件清单

创建 `plugin.json` 文件描述插件元数据。

### 步骤6：前端集成

在前端对应目录创建页面组件：
```
frontend_admin/src/views/your_plugin/
```

## 插件生命周期

### 加载阶段
1. 读取插件清单文件
2. 检查依赖关系
3. 执行数据库迁移
4. 初始化插件资源
5. 注册权限和菜单

### 启动阶段
1. 加载Controller
2. 初始化Service
3. 启动定时任务
4. 注册前端路由

### 停止阶段
1. 停止定时任务
2. 清理缓存
3. 释放资源
4. 注销路由

## 插件依赖管理

插件可以声明对其他插件的依赖：

```json
{
  "dependencies": [
    "com.linan.plugin.hr",
    "com.linan.plugin.system"
  ]
}
```

插件管理器会自动检查依赖关系，确保依赖插件先加载。

## 插件配置

插件可以在清单文件中声明配置项：

```json
{
  "configuration": {
    "your.feature.enabled": true,
    "your.feature.limit": 100
  }
}
```

这些配置项会在插件初始化时注入。

## 权限管理

每个插件需要声明自己的权限点：

```json
{
  "permissions": [
    "your:module:list",
    "your:module:add",
    "your:module:edit",
    "your:module:delete"
  ]
}
```

这些权限会自动注册到系统权限表中。

## 菜单管理

插件的菜单通过数据库迁移脚本注册：

```sql
INSERT INTO sys_menu (id, menu_name, parent_id, ...) VALUES
(10019, '你的模块', 0, 10, 'your', NULL, 1, 0, 'M', '0', '0', NULL, 'icon', 1, NOW(), NULL, NULL, '你的模块');
```

## 示例：工资管理插件

完整的工资管理插件示例已包含在项目中：

- 后端：`com.linan.plugin.salary`
- 数据库：`V36__salary_plugin.sql`
- 清单：`plugins/salary-plugin.json`

你可以参考这个示例创建自己的插件。

## 插件最佳实践

1. **命名规范**
   - 插件ID使用反向域名格式：`com.linan.plugin.xxx`
   - 类名使用清晰的前缀：`XxxPlugin`, `XxxService`

2. **版本管理**
   - 遵循语义化版本：`MAJOR.MINOR.PATCH`
   - 数据库迁移使用递增的版本号

3. **错误处理**
   - 使用 `PluginException` 抛出插件相关异常
   - 提供清晰的错误信息

4. **资源管理**
   - 在 `stop()` 方法中正确释放资源
   - 避免内存泄漏

5. **测试**
   - 为插件编写独立的测试用例
   - 测试插件的加载、启动、停止流程

## 插件管理API

插件管理器提供以下管理API：

```java
pluginManager.loadPlugin(plugin);      // 加载插件
pluginManager.startPlugin(pluginId);    // 启动插件
pluginManager.stopPlugin(pluginId);     // 停止插件
pluginManager.unloadPlugin(pluginId);   // 卸载插件
pluginManager.getPlugin(pluginId);      // 获取插件
pluginManager.getAllPlugins();         // 获取所有插件
```

## 前端插件化

前端同样支持插件化：

1. 创建插件目录：`frontend_admin/src/views/your_plugin/`
2. 创建页面组件
3. 在路由配置中注册插件路由
4. 在权限配置中添加插件权限

## 总结

BareZen_OA的插件化架构让你能够：

- * **快速开发**：专注于业务逻辑，无需修改核心代码
- * **独立部署**：插件可以独立开发、测试、部署
- * **灵活扩展**：按需添加功能，不影响现有系统
- * **易于维护**：每个插件职责单一，便于维护
- * **团队协作**：不同团队可以并行开发不同插件

就像乐高积木一样，每个插件都是独立的积木块，可以自由组合构建你需要的系统！
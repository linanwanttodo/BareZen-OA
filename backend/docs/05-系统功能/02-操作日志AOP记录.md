## 02-操作日志 AOP 记录实现

本模块通过 AOP（面向切面编程）技术，实现了对系统关键操作的自动审计与记录。

### 2.1 核心组件说明

| 组件名称 | 路径 | 作用 |
| :--- | :--- | :--- |
| **`@OperLog`** | `bz_framework_core/.../annotation/OperLog.java` | 标记在 Controller 方法上，定义日志的“模块标题”和“业务类型”。 |
| **`OperLogAspect`** | `bz_framework_core/.../aspect/OperLogAspect.java` | 拦截带注解的方法，利用 `@Around` 环绕通知记录执行耗时、操作用户及异常信息。 |
| **`SysOperLog`** | `bz_framework_core/.../entity/SysOperLog.java` | 对应数据库表 `sys_oper_log` 的实体类，继承自 `BaseEntity`。 |

### 2.2 使用示例

在 [SysUserController.java](file:///home/lin/桌面/BareZen_Base/backend/bz_web_app/src/main/java/com/linan/web/controller/SysUserController.java) 中：

```java
@OperLog(title = "用户管理", businessType = 1) // 1 代表新增
@RequirePermission("system:user:add")
@PostMapping
public Result<Void> save(@RequestBody SysUser user) {
    // 业务逻辑...
}
```

### 2.3 数据库部署

**重要**：首次使用前，请务必在项目数据库中执行以下脚本：
*   **脚本路径**：`sql/sys_oper_log.sql`

该表继承了 `BaseEntity`，因此会自动填充 `create_time`、`update_time` 以及 `del_flag`。

> **建议**：在生产环境中，建议将 `OperLogAspect` 中的保存逻辑改为 `@Async` 异步执行，以避免日志记录阻塞主业务流程。

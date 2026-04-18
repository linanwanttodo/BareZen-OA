# 06-AOP权限拦截实现

## 1. 功能概述

AuthInterceptor 只解决了"用户是否登录"的问题，没有解决"用户是否有权限操作"的问题。例如：
- 用户 A 登录后可以删除用户（管理员）
- 用户 B 登录后也可以看到删除按钮，但直接调用 API 时应当被拦截

AOP 权限拦截通过 Spring AOP 切面，在 Controller 方法执行前检查当前用户是否拥有该方法所声明的权限标识。

**完整的权限校验链**：

```text
HTTP 请求
  -> AuthInterceptor.preHandle()        校验 Token，确认用户已登录
    -> AOP 切面 @Around("@annotation(...)")  校验权限标识
      -> Controller 方法执行            执行业务逻辑
  <- 统一返回 Result 对象
```

---

## 2. 设计原理

### 2.1 AOP 术语说明

| 术语 | 说明 | 在权限场景中 |
| :--- | :--- | :--- |
| JoinPoint | 连接点，程序执行的某个点 | Controller 方法 |
| Pointcut | 切入点，定义哪些 JoinPoint 需要拦截 | 所有加了 @RequirePermission 的方法 |
| Advice | 通知，在 JoinPoint 上执行的逻辑 | 权限校验逻辑 |
| Aspect | 切面 = Pointcut + Advice 的组合 | 整个权限切面类 |

### 2.2 @RequirePermission 注解设计

```java
@Target(ElementType.METHOD)        // 只能标记在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，AOP 需要
public @interface RequirePermission {
    String[] value();  // 权限标识数组
}
```

**为什么是数组？**

`@RequirePermission("system:user:delete")` 表示需要单个权限。

`@RequirePermission({"system:user:add", "system:user:edit"})` 表示**同时拥有**两个权限才能访问（AND 关系）。

**AND vs OR 关系**：当前实现采用 AND 关系（用户必须同时拥有所有声明的权限）。如果需要 OR 关系（拥有任意一个即可），可以在注解中增加参数控制。

### 2.3 权限校验流程

```text
@RequirePermission("system:user:delete")
public Result<Void> delete(@PathVariable Long id) {
    // 方法执行前，AOP 切面检查：
    // 1. 获取当前登录用户
    Long userId = AuthInterceptor.getCurrentUserId();
    // 2. 查询用户的角色列表
    List<SysRole> roles = roleService.getRolesByUserId(userId);
    // 3. 根据角色查询权限标识集合
    Set<String> perms = new HashSet<>();
    for (SysRole role : roles) {
        perms.addAll(menuService.getPermsByRoleId(role.getId()));
    }
    // 4. 检查是否包含所需权限
    if (perms.contains("system:user:delete")) {
        return proceed();  // 有权限，放行
    } else {
        return Result.error(403, "没有访问权限");  // 无权限，拒绝
    }
}
```

**特殊情况：admin 角色**

如果用户拥有 `admin` 角色（超级管理员），应直接放行，无需逐条校验权限。这需要在切面中增加判断逻辑：

```java
// 切面中的判断逻辑
boolean isAdmin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleKey()));
if (isAdmin) {
    return joinPoint.proceed();  // admin 角色直接放行
}
```

---

## 3. 文件清单

| 文件路径 | 说明 |
| :--- | :--- |
| `.../core/security/RequirePermission.java` | 权限校验注解（已存在） |
| `.../core/security/aspect/RequirePermissionAspect.java` | AOP 权限切面（本章创建） |

---

## 4. 步骤一：创建 RequirePermission 注解

### 4.1 RequirePermission.java

文件路径：`bz_framework_core/src/main/java/com/linan/core/security/RequirePermission.java`

（此文件已存在，此处展示完整代码）

```java
package com.linan.core.security;

import java.lang.annotation.*;

/**
 * 权限校验注解
 *
 * 标记在 Controller 方法上，表示调用该接口需要用户拥有指定权限标识。
 * 配合 AOP 切面 RequirePermissionAspect，在方法执行前校验权限。
 *
 * 使用方式：
 * @RequirePermission("system:user:list")         // 单权限
 * @RequirePermission({"system:user:add", "system:user:edit"})  // 多权限（AND）
 *
 * 注意事项：
 * 1. 注解只在 Controller 方法上生效（@Target(ElementType.METHOD)）
 * 2. 如果用户拥有 admin 角色，切面会自动放行
 * 3. 权限标识需与 sys_menu 表中的 perms 字段一致
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /** 权限标识数组（AND 关系），如 {"system:user:list", "system:user:add"} */
    String[] value();
}
```

---

## 5. 步骤二：创建 AOP 权限切面

### 5.1 RequirePermissionAspect.java — 核心实现

文件路径：`bz_framework_core/src/main/java/com/linan/core/security/aspect/RequirePermissionAspect.java`

```java
package com.linan.core.security.aspect;

import com.linan.core.common.api.Result;
import com.linan.core.security.RequirePermission;
import com.linan.core.security.interceptor.AuthInterceptor;
import com.linan.system.entity.SysMenu;
import com.linan.system.entity.SysRole;
import com.linan.system.mapper.SysMenuMapper;
import com.linan.system.mapper.SysRoleMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AOP 权限校验切面
 *
 * 拦截所有标记了 @RequirePermission 注解的 Controller 方法，
 * 在执行前检查当前用户是否拥有所需的权限标识。
 *
 * 执行流程：
 * 1. 获取当前用户ID（从 AuthInterceptor 的 ThreadLocal）
 * 2. 查询用户的所有角色
 * 3. 检查是否为 admin 角色（是则直接放行）
 * 4. 查询所有角色的权限标识集合
 * 5. 检查是否包含方法要求的权限标识
 * 6. 全部包含则放行，否则返回 403
 *
 * @Aspect 声明此类为切面类
 * @Component 注册为 Spring Bean
 */
@Aspect
@Component
public class RequirePermissionAspect {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    /**
     * 切入点定义
     *
     * @annotation(RequirePermission) 匹配所有标记了 @RequirePermission 注解的方法
     */
    @Pointcut("@annotation(com.linan.core.security.RequirePermission)")
    public void requirePermissionPointcut() {}

    /**
     * 环绕通知
     *
     * @Around：在方法执行前后都可以插入逻辑
     * ProceedingJoinPoint：封装了被拦截方法的信息
     *
     * @param joinPoint 连接点
     * @return 方法执行结果（有权限）或 403 错误（无权限）
     */
    @Around("requirePermissionPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取当前登录用户ID
        Long currentUserId = AuthInterceptor.getCurrentUserId();
        if (currentUserId == null) {
            // 理论上不应该走到这里（AuthInterceptor 已拦截），但保留防御
            return Result.error(401, "未登录");
        }

        // 2. 从连接点获取被拦截的方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequirePermission annotation = signature.getMethod()
                .getAnnotation(RequirePermission.class);

        if (annotation == null) {
            return joinPoint.proceed();  // 无注解，放行
        }

        // 3. 获取方法要求的权限标识
        String[] requiredPerms = annotation.value();

        // 4. 查询当前用户的角色列表
        List<SysRole> roles = roleMapper.selectRolesByUserId(currentUserId);

        // 5. 检查是否为 admin 角色（超级管理员直接放行）
        boolean isAdmin = roles.stream()
                .anyMatch(r -> "admin".equals(r.getRoleKey()));
        if (isAdmin) {
            return joinPoint.proceed();
        }

        // 6. 收集用户所有权限标识
        Set<String> userPerms = new HashSet<>();
        for (SysRole role : roles) {
            List<String> perms = menuMapper.selectPermsByRoleId(role.getId());
            userPerms.addAll(perms);
        }

        // 7. 检查是否包含所有要求的权限
        boolean hasPermission = Arrays.stream(requiredPerms)
                .allMatch(userPerms::contains);

        if (hasPermission) {
            return joinPoint.proceed();  // 有权限，放行
        } else {
            // 无权限，返回 403
            return Result.error(403, "没有访问权限，请联系管理员");
        }
    }
}
```

**逐行讲解**：

- `@Aspect`：Spring AOP 注解，声明此类为切面类。Spring 容器会自动识别并处理。
- `@Pointcut("@annotation(...)")`：切入点表达式。`@annotation` 匹配带有指定注解的方法。与 execution 表达式不同，它更简洁，适合"只要有这个注解就拦截"的场景。
- `@Around`：环绕通知，功能最强大的通知类型。可以在方法执行前、执行后、异常时都插入逻辑。
- `ProceedingJoinPoint.proceed()`：执行被拦截的方法。如果不调用 proceed()，原方法就不会执行。
- `Arrays.stream(requiredPerms).allMatch(userPerms::contains)`：Stream 方式检查"用户权限集合是否包含所有要求的权限"。allMatch = AND 关系。

**为什么不在 Service 层做权限校验？**

| 位置 | 优点 | 缺点 |
| :--- | :--- | :--- |
| Controller 层 | 与 HTTP 接口一一对应，清晰明了 | Service 内部自调用无法拦截 |
| Service 层 | 业务逻辑级别校验，更安全 | 需要额外传递用户信息 |

当前采用 Controller 层校验，对标若依框架做法，且与 `@RequirePermission` 注解标记在 Controller 方法上更加直观。

---

## 6. 步骤三：在 Mapper 中添加查询方法

### 6.1 SysRoleMapper — 根据用户ID查询角色

文件路径：`bz_framework_core/src/main/java/com/linan/system/mapper/SysRoleMapper.java`

在现有 SysRoleMapper 中添加自定义方法：

```java
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色列表
     * 联表：sys_user_role + sys_role
     */
    @Select("SELECT r.* FROM sys_role r " +
            "LEFT JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND r.del_flag = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
```

### 6.2 SysMenuMapper — 根据角色ID查询权限

`selectPermsByRoleId` 已在 04-04 章节中添加到 SysMenuMapper，此处不需要重复。

---

## 7. 步骤四：使用注解

### 7.1 在 Controller 中添加 @RequirePermission

```java
@RestController
@RequestMapping("/api/system/v1/users")
public class SysUserController extends BaseController<SysUser, ISysUserService> {

    @RequirePermission("system:user:list")
    @GetMapping("/list")
    public Result<IPage<SysUser>> list(...) { ... }

    @RequirePermission("system:user:add")
    @PostMapping
    public Result<Void> save(@RequestBody SysUser user) { ... }

    @RequirePermission("system:user:edit")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) { ... }

    @RequirePermission("system:user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { ... }
}
```

---

## 8. 接口测试

```bash
# 1. 使用普通用户登录（假设用户只有 system:user:list 权限）
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username":"common","password":"123456"}' | jq -r '.data')

# 2. 尝试查询用户列表（有权限，应成功）
curl http://localhost:8080/api/system/v1/users/list \
  -H "Authorization: Bearer $TOKEN"
# 预期：{"code":200,"msg":"操作成功","data":{...}}

# 3. 尝试删除用户（无权限，应返回 403）
curl -X DELETE http://localhost:8080/api/system/v1/users/1 \
  -H "Authorization: Bearer $TOKEN"
# 预期：{"code":403,"msg":"没有访问权限，请联系管理员","data":null}
```

---

## 9. 常见问题

**Q: AOP 切面不生效的常见原因？**

A: 检查以下几点：
1. 切面类上必须标注 `@Aspect` 和 `@Component` 注解
2. 启动类必须扫描到切面所在的包（`"com.linan.core"` 已包含在 scanBasePackages 中）
3. 如果方法内部自调用（this.xxx()），AOP 不会生效，因为 Spring AOP 基于代理模式，只能拦截外部调用
4. 注意 `@annotation(com.linan.core.security.RequirePermission)` 引用的包路径是否正确

**Q: 为什么切面要检查 admin 角色并直接放行？**

A: admin 是超级管理员角色，应拥有系统所有权限。如果 admin 角色也需要配置权限标识，则需要给 admin 角色的 sys_role_menu 表插入所有菜单权限数据，维护成本高且容易遗漏。直接在代码中判断 admin 角色是业界通用做法。

**Q: 权限校验性能如何？**

A: 每次请求需要查询用户角色（2表联查）+ 查询权限标识（2表联查）。对于大部分管理系统，QPS 不高，性能不是瓶颈。如果是高并发场景，可以将用户权限缓存到 Redis 中（使用 userId 作为 key，权限集合作为 value，在登录时写入，角色变更时清除）。

**Q: @RequirePermission 和 @PassAuth 能同时使用吗？**

A: 不能。@PassAuth 表示不需要登录，@RequirePermission 表示需要权限校验。如果同时使用，以 @PassAuth 为准（AuthInterceptor 先判断 @PassAuth，直接放行，不会进入 AOP 切面）。

---

## 10. 本章完成检查清单

- [ ] 理解 AuthInterceptor 和 AOP 权限拦截的区别（登录 vs 授权）
- [ ] 理解 @RequirePermission 注解的 value 是多权限 AND 关系
- [ ] 理解 admin 角色的特殊处理（直接放行）
- [ ] RequirePermissionAspect 切面已创建
- [ ] SysRoleMapper.selectRolesByUserId 已添加
- [ ] 能正确地在 Controller 方法上添加 @RequirePermission 注解
- [ ] `./gradlew :bz_framework_core:compileJava` 编译通过

---

*下一章进入 [05-系统功能/01-数据字典管理](../05-系统功能/01-数据字典管理.md)*
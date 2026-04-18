# 05-用户登录与Token刷新

## 1. 功能概述

用户登录是进入系统的唯一入口。本章实现完整的登录认证流程，包括：
- 用户名密码校验（BCrypt 加密）
- JWT Token 生成与返回
- 密码加密 vs 明文存储
- Token 过期后的刷新策略

**完整的登录认证链路**：

```text
前端登录页  →  POST /api/auth/v1/login  →  AuthController.login()
  →  查询数据库验证用户名
  →  BCrypt 比对密码
  →  检查账号状态（正常/停用）
  →  生成 JWT Token（含 userId + username）
  →  返回 Token 给前端  →  前端存 localStorage
  →  后续请求自动带 Authorization Header
```

---

## 2. 设计原理

### 2.1 BCrypt 密码加密

**为什么不能存明文密码？**

```
明文存储的灾难：
数据库被拖库 → 全部密码泄露 → 用户在其他网站也用了相同密码 → 撞库攻击
```

BCrypt 是一种自适应哈希算法，具有以下特点：

| 特性 | 说明 | 效果 |
| :--- | :--- | :--- |
| 自动加盐 | 每次加密自动生成随机盐值 | 相同密码产生不同密文 |
| 可调强度 | rounds 参数控制迭代次数 | 默认 10 轮，每+1 加密时间翻倍 |
| 慢速哈希 | 故意设计为慢速 | 暴力破解成本极高 |
| 不可逆 | 无法从密文反推明文 | 只能比对，不能解密 |

**BCrypt 密文格式解析**：

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
│  │  │                     │
│  │  │                     └─ 盐值(22字符) + 哈希结果(31字符)
│  │  └─ 加密强度(log2(轮数))，10 = 2^10 = 1024轮迭代
│  └─ 算法版本，2a = BCrypt
└─ 算法标识符

# 同一密码(admin123)两次加密的结果：
$2a$10$salt1...结果A...   # 第一次加密，盐值=salt1
$2a$10$salt2...结果B...   # 第二次加密，盐值=salt2（完全不同！）
```

### 2.2 JWT Token 结构

JWT 由三部分组成，用 `.` 分隔：

```text
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJOYW1lIjoiYWRtaW4ifQ.xxx签名xxx
│                      │                                               │
│                      │                                               └─ Signature 签名
│                      └─ Payload 载荷（用户的业务数据）
└─ Header 头（算法 + Token类型）
```

**Payload 解码后**：

```json
{
  "userId": 1,
  "userName": "admin",
  "iat": 1714881600,   // 签发时间
  "exp": 1714888800    // 过期时间（2小时后）
}
```

### 2.3 Token 过期策略

| 策略 | 做法 | 优点 | 缺点 |
| :--- | :--- | :--- | :--- |
| 无刷新 | Token 过期就踢出 | 实现最简单 | 用户体验差 |
| 自动续期 | 每次请求延长 Token | 用户无感 | 永不登出，不安全 |
| 刷新Token | 短Token + 长RefreshToken | 安全 + 体验平衡 | 需双Token管理 |

本系统采用**短 Token（2小时）**策略，用户 Token 过期后需重新登录。

---

## 3. 步骤一：添加 BCrypt 依赖

### 3.1 build.gradle.kts

文件路径：`bz_framework_core/build.gradle.kts`

```kotlin
dependencies {
    // ... 保留原有依赖

    // Spring Security Crypto 模块
    // 注意：只引入 crypto 模块，不引入 spring-boot-starter-security
    // 如果引入完整的 Security Starter，会自动启用表单登录和权限拦截，
    // 与本项目自行实现的 JWT 鉴权体系冲突
    implementation("org.springframework.security:spring-security-crypto")
}
```

---

## 4. 步骤二：创建 PasswordUtil 工具类

### 4.1 PasswordUtil.java

文件路径：`bz_framework_core/src/main/java/com/linan/core/security/PasswordUtil.java`

```java
package com.linan.core.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密工具类
 *
 * 基于 BCrypt 算法，提供密码加密和比对功能。
 *
 * 为什么用静态方法而不是 Spring Bean？
 * PasswordUtil 是纯工具类，不依赖其他 Bean，不需要 Spring 管理。
 * 作为静态方法调用更方便：PasswordUtil.encode("admin123")
 * 相比 @Autowired PasswordEncoder 更轻量。
 */
public class PasswordUtil {

    /** BCrypt 密码编码器（线程安全，单例即可） */
    private static final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密明文密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt 密文（以 $2a$10$ 开头）
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证明文密码与密文是否匹配
     *
     * @param rawPassword     明文密码（用户输入的）
     * @param encodedPassword 数据库中的密文
     * @return true=匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
```

**测试验证**：

```java
// 加密测试
String hash = PasswordUtil.encode("admin123");
System.out.println(hash);  // $2a$10$N9qo8uLOickgx2ZMRZoMye...

// 比对测试（记住：同一密码每次加密结果不同，但都能匹配）
boolean ok1 = PasswordUtil.matches("admin123", hash);   // true
boolean ok2 = PasswordUtil.matches("wrong", hash);      // false

// 证明"自动加盐"的特性：
String hash1 = PasswordUtil.encode("admin123");
String hash2 = PasswordUtil.encode("admin123");
System.out.println(hash1.equals(hash2));  // false（每次盐值不同）
```

---

## 5. 步骤三：创建登录接口

### 5.1 AuthController.java

文件路径：`bz_framework_core/src/main/java/com/linan/system/controller/AuthController.java`

```java
package com.linan.system.controller;

import com.linan.core.common.api.Result;
import com.linan.core.security.*;
import com.linan.system.entity.SysUser;
import com.linan.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 登录认证 Controller
 *
 * @PassAuth：标记整个 Controller 跳过 Token 认证
 * 因为登录、注册等接口本身就是用来获取 Token 的，不需要 Token 才能访问。
 *
 * AuthInterceptor 中的判断逻辑：
 *   if (Controller 类上有 @PassAuth 注解) {
 *       return true;  // 跳过 Token 校验
 *   }
 */
@PassAuth                // 跳过 Token 鉴权
@RestController
@RequestMapping("/api/auth/v1")
public class AuthController {

    @Autowired
    private ISysUserService userService;

    /**
     * 用户登录
     *
     * POST /api/auth/v1/login
     * Content-Type: application/json
     * Body: {"username":"admin","password":"admin123"}
     *
     * 安全考量：
     * 1. 错误提示统一为"用户名或密码错误"（不透露是用户名不存在还是密码错误）
     *    防止攻击者通过不同的错误提示枚举有效用户名
     * 2. 未实现登录失败次数限制（应由接口限流实现，见 06-01 章节）
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 1. 参数校验
        if (username == null || username.isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }

        // 2. 查询用户
        // lambdaQuery() 是 MyBatis-Plus 提供的链式查询
        // eq(SysUser::getUserName, username) → WHERE user_name = 'admin'
        SysUser user = userService.lambdaQuery()
                .eq(SysUser::getUserName, username)
                .one();  // 查询单条记录

        // 3. 模糊提示（防止用户名枚举攻击）
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 4. 密码比对
        if (!PasswordUtil.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        // 5. 账号状态检查
        if ("0".equals(user.getStatus())) {
            return Result.error("该账号已被停用，请联系管理员");
        }

        // 6. 生成 JWT Token
        // 参数1: userId，参数2: username
        // Token 内部包含：{userId: 1, userName: "admin"}
        // 过期时间：2小时（在 JwtUtil 中配置）
        String token = JwtUtil.createToken(user.getId(), user.getUserName());

        // 7. 返回 Token
        return Result.success(token);
    }
}
```

---

## 6. 接口文档

### 6.1 登录接口

```text
请求方式：POST
请求URL：/api/auth/v1/login
请求头：Content-Type: application/json
请求体：
{
  "username": "admin",
  "password": "admin123"
}

成功响应：
{
  "code": 200,
  "msg": "操作成功",
  "data": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx"
}

失败响应（密码错误）：
{
  "code": 500,
  "msg": "用户名或密码错误",
  "data": null
}
```

---

## 7. 测试验证

```bash
# 1. 启动应用
./gradlew :bz_web_app:bootRun

# 2. 测试登录（预期成功）
curl -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 预期：{"code":200,"msg":"操作成功","data":"eyJhbG..."}

# 3. 测试错误密码（预期失败）
curl -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'

# 预期：{"code":500,"msg":"用户名或密码错误","data":null}

# 4. 解析 Token 查看内容（验证 JWT 是否包含 userId）
# 将返回的 Token 粘贴到 https://jwt.io 查看 Payload
```

---

## 8. 常见问题

**Q: 为什么不用 Spring Security 自带的登录功能？**

A: Spring Security 默认的表单登录和权限管理与本项目自行实现的 JWT + RBAC 体系冲突：
1. Security 默认使用 Session（Cookie），本项目使用 JWT（Header），两种方案不可混合
2. Security 的权限注解（@PreAuthorize）与本项目的 @RequirePermission 注解功能重叠
3. 自行实现可以完全掌控认证流程，减少"框架黑盒"问题

**Q: Token 过期后用户需要重新输入密码吗？**

A: 当前方案需要。Token 默认有效期 2 小时，过期后前端响应拦截器检测到 401，自动跳转登录页。可以实现的优化：
1. 引入 RefreshToken（有效期 7 天），静默刷新 AccessToken
2. 在 AuthInterceptor 中检测 Token 即将过期时，返回新的 Token 让前端替换

**Q: BCrypt 加密耗时多久？**

A: 默认 rounds=10，约 100ms 左右。对于登录场景完全可接受。可以调低 rounds（如 6，约 5ms）来加速，但会降低安全性。

**Q: 如何生成管理员密码的 BCrypt 密文？**

A: 临时写一段 Java 代码运行：
```java
public static void main(String[] args) {
    System.out.println(PasswordUtil.encode("admin123"));
    // 将输出的密文 INSERT 到 sys_user 表的 password 字段
}
```

---

## 9. 本章完成检查清单

- [ ] build.gradle.kts 已添加 spring-security-crypto 依赖
- [ ] PasswordUtil.java 已创建（BCrypt 加密和比对）
- [ ] AuthController.java 已创建（登录接口）
- [ ] @PassAuth 注解作用已理解（跳过 Token 校验）
- [ ] 理解 BCrypt 自动加盐原理（相同密码产生不同密文）
- [ ] 理解 JWT 三段式结构（Header.Payload.Signature）
- [ ] 用 curl 测试登录接口成功返回 Token
- [ ] `./gradlew :bz_framework_core:compileJava` 编译通过

---

*下一章：[06-AOP权限拦截实现](./06-AOP权限拦截实现.md)*
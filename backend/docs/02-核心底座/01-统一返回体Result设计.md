# 01-统一返回体Result设计

## 1. 功能概述

`Result<T>` 是 BareZen_OA 后端所有接口的统一返回体格式。无论接口是成功还是失败，返回给前端的 JSON 结构始终保持一致。

**解决的问题**：
- 前端不需要根据接口不同而编写不同的响应解析逻辑
- 错误信息可以统一处理和展示
- 便于 AOP 拦截器在切面中统一处理返回值和异常

**对比**：

```text
# 没有统一返回体（不推荐）
/api/user       -> { "userName": "admin", ... }
/api/user/error -> { "error": "xxx" }
/api/role       -> { "success": true, "data": [...] }

# 有统一返回体（BareZen_OA 做法）
所有接口       -> { "code": 200, "msg": "操作成功", "data": { ... } }
```

---

## 2. 设计原理

### 2.1 泛型设计

`Result<T>` 使用 Java 泛型，使得 `data` 字段可以是任意类型：

- `Result<User>` -> data 是 User 对象
- `Result<List<User>>` -> data 是 User 列表
- `Result<PageData<User>>` -> data 是分页对象
- `Result<Void>` -> data 为 null（删除操作等无返回数据场景）

### 2.2 静态工厂方法

我们不使用 `new Result<>()` 直接构造对象，而是通过静态工厂方法创建：

- `Result.success(data)` — 成功响应
- `Result.error("错误信息")` — 失败响应
- `Result.error(ResultCodeEnum.XXX)` — 带状态码的失败响应

静态工厂方法的优势：
- 方法名表达意图，代码可读性更高
- 封装构造逻辑，调用方无需关心 code/msg/data 的组合规则
- 便于在工厂方法中添加日志、链路追踪等通用逻辑

### 2.3 状态码枚举分离

将状态码定义在 `ResultCodeEnum` 枚举中，而非硬编码在 `Result` 类中：

- 状态码集中管理，一目了然
- 新增状态码时只需修改枚举类
- 避免魔术数字（magic number）散布在代码各处

---

## 3. 文件清单

| 文件路径 | 说明 |
| :--- | :--- |
| `bz_framework_core/src/main/java/com/linan/core/common/api/ResultCodeEnum.java` | 统一状态码枚举 |
| `bz_framework_core/src/main/java/com/linan/core/common/api/Result.java` | 统一返回体类 |

---

## 4. 代码实现

### 4.1 ResultCodeEnum.java — 统一状态码枚举

文件路径：`bz_framework_core/src/main/java/com/linan/core/common/api/ResultCodeEnum.java`

```java
package com.linan.core.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一状态码枚举
 *
 * 定义所有可能返回给前端的状态码及其对应的提示信息。
 * 前端根据状态码决定后续行为：
 * - 200：正常处理 data 展示数据
 * - 400：提示用户修正参数后重试
 * - 401：跳转到登录页面
 * - 403：提示权限不足
 * - 500：提示系统异常
 */
@Getter                     // Lombok 注解：自动生成所有字段的 getter 方法
@AllArgsConstructor         // Lombok 注解：自动生成包含所有字段的全参构造函数
public enum ResultCodeEnum {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 未登录或 Token 已过期
     * 前端收到此状态码后应清除 token 并跳转到登录页面
     */
    UNAUTHORIZED(401, "未登录或Token已过期"),

    /**
     * 没有访问权限
     * 用户已登录但不具备访问当前接口的权限
     */
    FORBIDDEN(403, "没有访问权限"),

    /**
     * 参数校验失败
     * 请求参数格式错误、缺少必填项、数据类型不匹配等
     */
    VALIDATE_FAILED(400, "参数检验失败"),

    /**
     * 系统内部异常
     * 未知的运行时错误，需要运维人员关注
     */
    FAILED(500, "系统异常，请稍后重试");

    /*
     * 状态码数值
     */
    private final int code;

    /*
     * 状态码对应的提示信息
     */
    private final String msg;
}
```

**逐行讲解**：

- `package com.linan.core.common.api;`：所有统一 API 相关的类放在此包中，包括返回体、状态码、分页对象等。
- `@Getter`：Lombok 注解，编译时自动生成 `getCode()` 和 `getMsg()` 两个方法。枚举字段是 `final` 的，所以不需要生成 setter。
- `@AllArgsConstructor`：Lombok 注解，自动生成包含 `int code` 和 `String msg` 两个参数的全参构造函数。因为枚举的每个值都在声明时调用此构造函数（如 `SUCCESS(200, "操作成功")`）。
- `public enum ResultCodeEnum`：Java 枚举类型，每个实例是单例的，天然线程安全。
- `SUCCESS(200, "操作成功")`：每个枚举值的括号内参数会自动传入构造函数。

**使用方式**：
```java
ResultCodeEnum.SUCCESS.getCode()  // 返回 200
ResultCodeEnum.SUCCESS.getMsg()   // 返回 "操作成功"
```

### 4.2 Result.java — 统一返回体

文件路径：`bz_framework_core/src/main/java/com/linan/core/common/api/Result.java`

```java
package com.linan.core.common.api;

import lombok.Data;

/**
 * 统一API返回体
 *
 * 所有 Controller 方法的返回值都必须使用 Result<T> 包装。
 * T 为 data 的实际数据类型，可以是：
 * - 普通对象：Result<User>
 * - 集合：Result<List<User>>
 * - 分页对象：Result<PageData<User>>
 * - 无数据：Result<Void>（此时 data 为 null）
 *
 * @param <T> 数据类型（泛型）
 */
@Data
public class Result<T> {

    /**
     * 状态码
     * 200 表示成功，其他值表示异常。
     * 前端根据此字段决定后续行为（见 ResultCodeEnum 注释）。
     */
    private int code;

    /**
     * 提示信息
     * 成功时为"操作成功"，失败时为具体的错误描述。
     * 前端可直接将此信息展示给用户（如 Element Plus 的 ElMessage）。
     */
    private String msg;

    /**
     * 返回数据
     * 成功时包含业务数据，失败时为 null。
     * 使用泛型 T 存储任意类型的数据。
     */
    private T data;

    /**
     * 无参构造函数
     * 使用 protected 修饰，禁止外部直接 new Result()。
     * 外部必须通过静态工厂方法（success/error）创建实例。
     * protected 允许子类继承（如果有需要）和框架反序列化（Jackson 需要）。
     */
    protected Result() {}

    /**
     * 全参构造函数
     * 同样使用 protected，确保构造逻辑集中在静态工厂方法中。
     *
     * @param code 状态码
     * @param msg  提示信息
     * @param data 返回数据
     */
    protected Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应 — 使用默认成功信息
     *
     * 使用方式：
     * Result<User> result = Result.success(user);
     * 返回给前端：{"code":200,"msg":"操作成功","data":{...user对象...}}
     *
     * @param data 返回的业务数据
     * @param <T>  数据类型（由参数推导）
     * @return Result 对象，code=200，msg="操作成功"
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMsg(), data);
    }

    /**
     * 成功响应 — 使用自定义成功信息
     *
     * 使用方式：
     * Result<User> result = Result.success("用户创建成功", user);
     * 返回给前端：{"code":200,"msg":"用户创建成功","data":{...user对象...}}
     *
     * @param msg  自定义成功提示信息
     * @param data 返回的业务数据
     * @param <T>  数据类型
     * @return Result 对象，code=200，msg 为自定义信息
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), msg, data);
    }

    /**
     * 失败响应 — 使用预定义状态码
     *
     * 使用方式：
     * Result<Void> result = Result.error(ResultCodeEnum.UNAUTHORIZED);
     * 返回给前端：{"code":401,"msg":"未登录或Token已过期","data":null}
     *
     * @param errorCode 错误状态码枚举值
     * @param <T>       数据类型（通常为 Void）
     * @return Result 对象，code 和 msg 来自枚举
     */
    public static <T> Result<T> error(ResultCodeEnum errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    /**
     * 失败响应 — 使用自定义错误信息
     *
     * 使用方式：
     * Result<Void> result = Result.error("用户名不能为空");
     * 返回给前端：{"code":500,"msg":"用户名不能为空","data":null}
     *
     * @param msg 自定义错误提示信息
     * @param <T> 数据类型（通常为 Void）
     * @return Result 对象，code=500，msg 为自定义信息
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCodeEnum.FAILED.getCode(), msg, null);
    }
}
```

**逐行讲解**：

- `@Data`：Lombok 的组合注解，等价于同时使用 `@Getter`、`@Setter`、`@ToString`、`@EqualsAndHashCode`、`@RequiredArgsConstructor`。编译时自动生成所有字段的 getter/setter 方法，以及 toString/hashCode/equals 等方法。

- `public class Result<T>`：类声明中的 `<T>` 是泛型参数，表示 data 字段的类型由调用方决定。

- `protected Result() {}`：无参构造器使用 protected 修饰的理由：
  1. 禁止外部直接 `new Result()` 创建空对象，防止 data 为 null 且 code 未初始化的情况。
  2. Jackson（Spring Boot 默认的 JSON 序列化库）反序列化 JSON 时需要无参构造器。虽然项目中不常用 Jackson 反序列化 Result（因为 Result 是返回值而非请求体），但保留无参构造器是一个好习惯。
  3. protected 允许子类和同包类访问，不会完全断绝扩展性。

- `public static <T> Result<T> success(T data)`：
  - `static` 表示静态方法，无需实例即可调用。
  - `<T>` 在返回值前声明泛型参数，因为静态方法不能直接使用类声明的泛型。
  - 方法内通过 `new Result<>(..., ..., data)` 创建实例，泛型 T 由参数 `data` 的类型自动推导。

- `Result.error(ResultCodeEnum errorCode)` 和 `Result.error(String msg)`：
  两个重载版本的 error 方法：
  - 前者使用预定义的枚举状态码（如 401 UNAUTHORIZED），code 和 msg 都来自枚举。
  - 后者使用自定义错误信息（如"用户名不能为空"），code 固定为 500（FAILED），msg 为传入的字符串。
  - 两个方法都返回 `data=null`，因为错误时通常不需要返回业务数据。

---

## 5. 在 Controller 中使用

```java
package com.linan.web.controller;

import com.linan.core.common.api.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo/v1")
public class DemoController {

    /**
     * 返回单个对象
     * 请求：GET /api/demo/v1/user/1
     * 响应：{"code":200,"msg":"操作成功","data":{"id":1,"name":"admin"}}
     */
    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    /**
     * 返回列表
     * 请求：GET /api/demo/v1/users
     * 响应：{"code":200,"msg":"操作成功","data":[{...},{...}]}
     */
    @GetMapping("/users")
    public Result<List<User>> listUsers() {
        List<User> users = userService.listAll();
        return Result.success(users);
    }

    /**
     * 创建资源 — 使用自定义成功信息
     * 请求：POST /api/demo/v1/user  Body: {...}
     * 响应：{"code":200,"msg":"用户创建成功","data":{...刚创建的用户对象...}}
     */
    @PostMapping("/user")
    public Result<User> createUser(@RequestBody User user) {
        userService.save(user);
        return Result.success("用户创建成功", user);
    }

    /**
     * 删除资源 — 无返回数据
     * 请求：DELETE /api/demo/v1/user/1
     * 响应：{"code":200,"msg":"操作成功","data":null}
     */
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success(null);  // null 可省略，直接 Result.success() 同理
    }

    /**
     * 业务校验失败 — 返回自定义错误
     * 响应：{"code":500,"msg":"用户名已存在","data":null}
     */
    @PostMapping("/user/validate")
    public Result<Void> validateUser(@RequestBody User user) {
        if (userService.existsByUserName(user.getUserName())) {
            return Result.error("用户名已存在");
        }
        return Result.success(null);
    }
}
```

---

## 6. 接口返回体示例（供前端对接参考）

### 6.1 成功返回示例

```json
// 单个对象
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "userId": 1,
    "userName": "admin",
    "nickName": "超级管理员"
  }
}

// 列表
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    { "userId": 1, "userName": "admin" },
    { "userId": 2, "userName": "user1" }
  ]
}

// 无数据（删除操作）
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 6.2 失败返回示例

```json
// 参数校验失败
{
  "code": 400,
  "msg": "参数检验失败",
  "data": null
}

// 未登录
{
  "code": 401,
  "msg": "未登录或Token已过期",
  "data": null
}

// 权限不足
{
  "code": 403,
  "msg": "没有访问权限",
  "data": null
}

// 系统异常
{
  "code": 500,
  "msg": "系统异常，请稍后重试",
  "data": null
}
```

---

## 7. 前端 TypeScript 类型定义（参考）

前端可以根据此规范定义 TypeScript 类型：

```typescript
// 统一返回体类型
interface Result<T = any> {
  code: number
  msg: string
  data: T
}

// 分页返回体类型
interface PageData<T> {
  total: number
  pageNum: number
  pageSize: number
  pages: number
  list: T[]
}

// 使用示例
const response: Result<User> = await api.getUser(1)
if (response.code === 200) {
  const user = response.data  // 类型为 User
}
```

---

## 8. 测试验证

### 8.1 单元测试

```java
package com.linan.core.common.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void testSuccess() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void testSuccessWithCustomMsg() {
        Result<String> result = Result.success("创建成功", "hello");
        assertEquals(200, result.getCode());
        assertEquals("创建成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void testErrorWithCode() {
        Result<Void> result = Result.error(ResultCodeEnum.UNAUTHORIZED);
        assertEquals(401, result.getCode());
        assertEquals("未登录或Token已过期", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithMsg() {
        Result<Void> result = Result.error("用户名已存在");
        assertEquals(500, result.getCode());
        assertEquals("用户名已存在", result.getMsg());
        assertNull(result.getData());
    }
}
```

### 8.2 运行测试

```bash
./gradlew :bz_framework_core:test --tests "com.linan.core.common.api.ResultTest"
```

---

## 9. 常见问题

**Q: 为什么 `data` 不使用 Optional<T>？**

A: Optional 设计用于方法返回值而非字段。JSON 序列化时 Optional 会产生嵌套结构，不符合前端预期。直接用 `T data = null` 更简洁。

**Q: 成功返回时 code 一定是 200 吗？**

A: 是的。本框架中，所有正常响应（包括查询到空列表、删除不存在的记录等场景）都返回 code=200。HTTP 状态码也固定为 200。错误信息通过 `msg` 字段传递，而非通过 HTTP 状态码区分。

**Q: 可以扩展更多的状态码枚举值吗？**

A: 可以。直接在 `ResultCodeEnum` 中添加新的枚举值即可，例如：
```java
USER_NOT_EXIST(1001, "用户不存在"),
USER_NAME_DUPLICATE(1002, "用户名已存在"),
```

**Q: `@Data` 注解生成了 setter，这不是破坏了不可变性吗？**

A: 是的。如果追求严格不可变性，可以仅使用 `@Getter` 替代 `@Data`。本项目使用 `@Data` 的原因是 Jackson 序列化时需要 setter（某些配置下），且对于简单的 DTO 类，不可变性的收益不大。

---

## 10. 本章完成检查清单

- [ ] 理解 `Result<T>` 泛型设计的意义
- [ ] 理解静态工厂方法相对于直接构造的优势
- [ ] 理解状态码枚举分离的设计原理
- [ ] 理解 `protected` 构造函数的用途
- [ ] 能在 Controller 中正确使用 `Result.success()` 和 `Result.error()`

---

*下一章：[02-全局异常处理机制](./02-全局异常处理机制.md)*
# 01-RESTful接口设计规范

## 1. 概述

本规范定义了 BareZen_Base（BareZen_Base）项目中所有后端 RESTful API 的设计标准。无论是核心底座提供的通用接口，还是业务模块开发的具体接口，都必须严格遵守本规范。

制定统一接口规范的目的是：

- **降低前后端沟通成本**：前端开发人员只需理解一套约定，即可对接所有接口。
- **保证系统一致性**：避免不同开发人员写出风格各异的接口。
- **便于自动化测试**：统一的格式可以使用通用脚本进行批量测试。
- **提升可维护性**：新成员通过阅读规范即可快速上手。

---

## 2. URL命名规范

### 2.1 基本格式

```
/模块标识/版本号/资源名称
```

各部分说明：

| 组成部分 | 说明 | 示例 |
| :--- | :--- | :--- |
| 模块标识 | 标识接口所属的功能模块 | `system`（系统管理）、`monitor`（监控管理） |
| 版本号 | API 版本号，通常为 `v1` | `v1` |
| 资源名称 | 操作的资源对象，使用小写复数名词 | `users`、`roles`、`menus` |

### 2.2 完整URL示例

```
GET    /api/system/v1/users          # 查询用户列表
POST   /api/system/v1/users          # 新增用户
GET    /api/system/v1/users/1        # 查询ID为1的用户
PUT    /api/system/v1/users/1        # 更新ID为1的用户
DELETE /api/system/v1/users/1        # 删除ID为1的用户
```

### 2.3 命名规则

- 全部使用**小写字母**，单词之间用短横线 `-` 分隔（不使用下划线或驼峰）。
- 资源名称使用**复数形式**（`users` 而非 `user`）。
- URL 末尾**不要**添加斜杠 `/`。
- 避免使用动词，用 HTTP 方法表达动作。例如：
  - 错误：`POST /api/system/v1/createUser`
  - 正确：`POST /api/system/v1/users`

以下特殊操作可以使用动词形式的结尾：

```
POST /api/system/v1/users/1/reset-password   # 重置密码
POST /api/system/v1/users/1/change-status    # 修改状态
```

---

## 3. HTTP方法约定

BareZen_Base 接口严格遵循 RESTful 风格，使用以下 HTTP 方法：

| HTTP方法 | 用途 | 是否幂等 | 说明 |
| :--- | :--- | :---: | :--- |
| `GET` | 查询资源 | 是 | 请求参数放在 URL Query String 中，不在 Body 中传参 |
| `POST` | 创建资源 | 否 | 请求参数放在 Request Body 中（JSON格式） |
| `PUT` | 完整更新资源 | 是 | 请求参数放在 Request Body 中（JSON格式） |
| `DELETE` | 删除资源 | 是 | 资源ID通过 URL 路径传递 |
| `PATCH` | 部分更新资源 | 否 | 仅传递需要更新的字段（项目中暂未大量使用） |

### 3.1 方法选择决策树

```text
操作是创建新资源？
  ├── 是 → POST
  └── 否 → 操作是获取数据？
            ├── 是 → GET
            └── 否 → 操作是删除？
                      ├── 是 → DELETE
                      └── 否 → PUT 或 PATCH（更新）
```

---

## 4. 统一返回体格式

### 4.1 返回体结构

所有接口（无论成功或失败）都必须返回以下 JSON 结构：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| `code` | int | 是 | 状态码，200 表示成功，其他值表示异常 |
| `msg` | String | 是 | 提示信息，成功时为"操作成功"，失败时为具体错误描述 |
| `data` | T（泛型） | 否 | 返回的数据体，可以是对象、数组或 null |

### 4.2 成功响应示例

**查询单个对象时**，`data` 为对象：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "userId": 1,
    "userName": "admin",
    "nickName": "超级管理员",
    "status": "1"
  }
}
```

**查询列表时**，`data` 为数组：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    { "userId": 1, "userName": "admin" },
    { "userId": 2, "userName": "user1" }
  ]
}
```

**分页查询时**，`data` 为分页对象（参见第5节）。

**无返回数据时**（如删除操作），`data` 为 null：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 4.3 失败响应示例

```json
{
  "code": 400,
  "msg": "用户名不能为空",
  "data": null
}
```

```json
{
  "code": 401,
  "msg": "未登录或Token已过期",
  "data": null
}
```

```json
{
  "code": 500,
  "msg": "系统异常，请稍后重试",
  "data": null
}
```

### 4.4 状态码枚举

| 状态码 | 常量名 | 说明 | 使用场景 |
| :---: | :--- | :--- | :--- |
| 200 | SUCCESS | 操作成功 | 所有成功请求 |
| 400 | VALIDATE_FAILED | 参数校验失败 | 请求参数不合法、缺少必填项 |
| 401 | UNAUTHORIZED | 未登录或Token已过期 | Token缺失或失效 |
| 403 | FORBIDDEN | 没有访问权限 | 已登录但无此接口权限 |
| 500 | FAILED | 系统异常 | 未知的内部错误 |

**重要规则**：前端会根据 `code` 值决定后续行为：

- `200`：正常解析 `data` 展示数据。
- `400`：提示 `msg` 中的错误信息让用户修正后重试。
- `401`：自动跳转到登录页面。
- `403`：提示权限不足。
- `500`：提示"系统异常"并记录日志。

---

## 5. 分页参数规范

### 5.1 请求参数

所有需要分页的 `GET` 列表查询接口，统一使用以下 Query 参数：

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :---: | :---: | :--- |
| `pageNum` | int | 否 | 1 | 当前页码，从1开始 |
| `pageSize` | int | 否 | 10 | 每页显示条数 |
| `orderBy` | String | 否 | 空 | 排序字段名（需白名单校验） |
| `orderType` | String | 否 | "desc" | 排序方式，"asc" 或 "desc" |

示例请求：

```
GET /api/system/v1/users?pageNum=1&pageSize=20&orderBy=createTime&orderType=desc
```

### 5.2 分页响应格式

分页查询的 `data` 字段使用以下结构：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 5,
    "list": [
      { "userId": 1, "userName": "admin" },
      { "userId": 2, "userName": "user1" }
    ]
  }
}
```

分页对象字段说明：

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `total` | long | 总记录数 |
| `pageNum` | int | 当前页码 |
| `pageSize` | int | 每页显示条数 |
| `pages` | int | 总页数，计算公式：(total + pageSize - 1) / pageSize |
| `list` | Array | 当前页数据列表 |

前端分页组件需要的核心字段为 `total`、`pageNum`、`pageSize` 和 `list`，Element Plus 的 `el-pagination` 组件可直接对接此格式。

---

## 6. 请求规范

### 6.1 请求头规定

| Header名称 | 必填 | 说明 |
| :--- | :---: | :--- |
| `Content-Type` | 是 | 统一使用 `application/json`，文件上传时使用 `multipart/form-data` |
| `Authorization` | 视情况 | 格式为 `Bearer {token}`，除 `@PassAuth` 标记的公开接口外，所有接口必须携带 |
| `Accept` | 推荐 | 建议设置为 `application/json` |

### 6.2 Token 传递规范

所有需要登录的接口，客户端必须在请求头中携带 JWT Token：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
```

说明：
- 登录成功后，后端在 `data` 中返回 Token，前端存储到 localStorage。
- 前端 Axios 拦截器自动在每个请求的 Header 中注入该 Token。
- Token 过期后，后端返回 401，前端自动跳转登录页。

### 6.3 POST/PUT 请求体格式

请求体统一使用 `application/json` 格式。参数命名采用**驼峰命名法**（camelCase），与 Java 实体类字段保持一致：

```json
{
  "userName": "admin",
  "nickName": "超级管理员",
  "password": "123456",
  "email": "admin@example.com",
  "roleIds": [1, 2]
}
```

注意事项：
- 日期时间字段统一使用 ISO 8601 格式字符串（如 `"2026-05-05 14:30:00"`）。
- 布尔值使用 `true` / `false`（而非 1 / 0）。
- 空值字段统一返回 `null`，不要使用空字符串 `""` 替换。
- 数值类型字段不使用引号包裹。

---

## 7. 接口模块划分

BareZen_Base 后端按功能域将接口划分为以下模块：

| 模块路径 | 说明 | 示例 URL |
| :--- | :--- | :--- |
| `/api/auth/v1/` | 登录认证相关（公开接口） | `POST /api/auth/v1/login` |
| `/api/system/v1/` | 系统管理（用户/角色/菜单/部门/岗位） | `GET /api/system/v1/users` |
| `/api/monitor/v1/` | 监控管理（在线用户/操作日志/登录日志） | `GET /api/monitor/v1/oper-log` |
| `/api/common/v1/` | 通用接口（字典/参数/文件上传） | `GET /api/common/v1/dict-data` |

### 7.1 公开接口（无需Token）

以下接口使用 `@PassAuth` 注解标记，后端拦截器会直接放行：

```
POST /api/auth/v1/login              # 用户登录
POST /api/auth/v1/register           # 用户注册（如有）
GET  /api/auth/v1/captcha            # 获取验证码
GET  /api/common/v1/health           # 健康检查
```

### 7.2 授权接口（需要Token + 权限）

除上述公开接口外，其他所有接口必须携带有效 Token，且可通过 `@RequirePermission` 注解进一步限制权限。

---

## 8. 错误码规范

### 8.1 系统级错误码（固定）

| code | 说明 | 触发条件 |
| :---: | :--- | :--- |
| 200 | 操作成功 | 正常响应 |
| 400 | 参数校验失败 | `@Validated` 校验不通过、请求格式错误 |
| 401 | 未登录或Token已过期 | 未携带Token、Token为空、Token解析失败 |
| 403 | 没有访问权限 | 已登录但无接口所需权限标识 |
| 404 | 资源不存在 | 查询或操作了不存在的记录 |
| 405 | 请求方法不支持 | 使用了错误的 HTTP 方法 |
| 500 | 系统异常 | 未捕获的内部错误 |

### 8.2 业务错误码规范（可扩展）

当需要返回更具体的业务错误时，在 `ResultCodeEnum` 中扩展自定义状态码：

```java
// 示例：业务自定义错误码
USER_NOT_EXIST(1001, "用户不存在"),
USER_NAME_DUPLICATE(1002, "用户名已存在"),
ROLE_NOT_EXIST(2001, "角色不存在"),
MENU_HAS_CHILDREN(3001, "菜单下存在子菜单，无法删除"),
```

建议的编码区间：

| 区间 | 模块 |
| :---: | :--- |
| 1001-1999 | 用户模块 |
| 2001-2999 | 角色模块 |
| 3001-3999 | 菜单模块 |
| 4001-4999 | 部门/岗位模块 |
| 5001-5999 | 字典/参数模块 |
| 6001-6999 | 文件/存储模块 |

---

## 9. 登录认证接口规范

### 9.1 登录接口

**请求**：
```
POST /api/auth/v1/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456",
  "captcha": "abcd",       // 验证码（如果有）
  "uuid": "captcha-uuid"   // 验证码唯一标识（如果有）
}
```

**成功响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4ifQ.xxx"
  }
}
```

前端拿到 `token` 后应：
1. 存储到 `localStorage.setItem('token', token)`
2. 在后续所有请求的 Header 中携带 `Authorization: Bearer {token}`
3. 跳转到系统首页

**失败响应**：
```json
{
  "code": 400,
  "msg": "用户名或密码错误",
  "data": null
}
```

### 9.2 获取当前用户信息接口

**请求**：
```
GET /api/system/v1/users/current/info
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx
```

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "user": {
      "userId": 1,
      "userName": "admin",
      "nickName": "超级管理员",
      "avatar": "/profile/avatar/2025/01/01/avatar.png"
    },
    "roles": ["admin"],
    "permissions": ["system:user:list", "system:user:add", "system:user:edit", "system:user:delete"]
  }
}
```

说明：
- `roles` 数组：当前用户的角色标识集合，前端用于判断显示哪些页面模块。
- `permissions` 数组：当前用户的权限字符串集合，前端用于判断页面中显示哪些按钮。

### 9.3 获取路由菜单接口

**请求**：
```
GET /api/system/v1/menus/routers
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.xxx
```

**响应**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "System",
      "path": "/system",
      "component": "Layout",
      "meta": {
        "title": "系统管理",
        "icon": "system",
        "noCache": false
      },
      "children": [
        {
          "id": 2,
          "parentId": 1,
          "name": "User",
          "path": "user",
          "component": "system/user/index",
          "meta": {
            "title": "用户管理",
            "icon": "user"
          }
        }
      ]
    }
  ]
}
```

此接口返回树形结构的菜单数据，前端 Vue Router 可直接根据此数据动态生成路由。

---

## 10. 前后端协作约定

### 10.1 登录流程时序

```text
前端                                后端
 |                                  |
 |--- POST /api/auth/v1/login ----->|
 |    { username, password }        |
 |                                  |--- 验证用户名密码
 |                                  |--- 生成 JWT Token
 |<-- { code:200, data:{token} } ---|
 |                                  |
 |--- 存储 token 到 localStorage
 |--- GET /api/system/v1/users/current/info -->|
 |    Authorization: Bearer {token}           |
 |                            查询用户信息 <---|
 |<-- { user, roles, permissions } -----------|
 |                                  |
 |--- GET /api/system/v1/menus/routers ------>|
 |    Authorization: Bearer {token}           |
 |                             查询菜单树 <---|
 |<-- [{...菜单路由数据...}] ----------------|
 |                                  |
 |--- 根据菜单数据动态生成前端路由
 |--- 跳转到首页
```

### 10.2 前端 Axios 拦截器约定

前端需要在 Axios 实例中配置两个拦截器：

**请求拦截器**：
- 自动从 localStorage 读取 token。
- 在每个请求的 `Authorization` Header 中注入 `Bearer {token}`。

**响应拦截器**：
- 如果 `code === 401`，清除 token 并跳转到登录页。
- 如果 `code !== 200`，弹出 `msg` 错误提示。
- 如果 `code === 200`，正常返回 `data` 给调用方。

---

## 11. 开发联调指南

### 11.1 使用 Knife4j

项目集成了 Knife4j 接口文档，启动后端服务后访问：

```
http://localhost:8080/doc.html
```

可以在线查看所有接口的说明，并直接在页面上进行测试调用。

**依赖**：`knife4j-openapi3-jakarta-spring-boot-starter` 已添加到 `bz_web_app/build.gradle.kts`。

### 11.2 使用 curl 测试

```bash
# 测试登录
curl -X POST http://localhost:8080/api/auth/v1/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 测试带Token的请求（将{token}替换为上一步返回的token值）
curl -X GET http://localhost:8080/api/system/v1/users?pageNum=1\&pageSize=10 \
  -H "Authorization: Bearer {token}"
```

### 11.3 使用 Postman 测试

1. 创建 Collection，在 Collection 级别的 Authorization 中配置 `Bearer Token`。
2. 在 Tests 脚本中提取登录接口返回的 Token 并设置为环境变量。
3. 后续接口直接引用该环境变量。

---

## 12. 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
| :--- | :--- | :--- | :--- |
| 2026-05-05 | v1.0 | 初始版本，定义 RESTful 接口规范 | BareZen_Base Team |

---

*下一章：[01-环境搭建与工程初始化](../01-基础环境/01-环境搭建与工程初始化.md)*

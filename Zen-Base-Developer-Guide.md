# BareZen_Base 模块化全栈框架：开发落地执行手册

## 项目完成度

| 模块 | 完成度 | 说明 |
| :--- | :--- | :--- |
| 核心底座 `bz_framework_core` | 100% | Result, JWT, AOP, 异常处理 |
| RBAC 权限体系 | 100% | 五表、@RequirePermission、AuthInterceptor |
| 文件管理 | 100% | 上传、下载、删除 |
| 数据字典 | 100% | Redis 缓存 |
| 通知公告 | 100% | CRUD + 发布/下架 |
| 操作日志 | 100% | @OperLog AOP 记录 |
| 接口限流 | 100% | Redis + Lua 脚本 |
| 防重复提交 | 100% | Redis SETNX |
| 在线用户监控 | 100% | Redis 在线记录、强制下线 |
| Dockerfile 部署 | 100% | 多阶段构建 |
| Knife4j 接口文档 | 100% | 访问 `/doc.html` |
| 前端 (Vue) | 全部完成 | Admin 55文件 + User 29文件，均可 build + dev |
| 系统监控 | 100% | 在线用户监控（后端） |

## 技术栈

- **后端**：Spring Boot 3.2.4 + Java 21 + Gradle + MyBatis-Plus 3.5.7
- **安全**：JWT + BCrypt + AOP 权限
- **缓存**：Redis 7.0
- **数据库**：MySQL 8.0
- **文档**：Knife4j
- **部署**：Docker

## 项目结构

```
backend/
├── build.gradle.kts          # 根构建：插件版本、subprojects 公共依赖
├── settings.gradle.kts       # 定义子模块
├── docker-compose.yml       # MySQL + Redis
├── Dockerfile                # 容器化部署
├── Makefile                 # 快捷命令
├── bz_framework_core/       # 核心底座模块 (jar)
│   └── src/main/java/com/linan/
│       ├── core/            # 基础设施（Result, JWT, AOP, Config）
│       └── system/           # 系统业务（Entity, Mapper, Service, Controller）
└── bz_web_app/             # 启动模块 (bootJar)
    └── src/main/java/com/linan/
        └── web/             # Web 控制器（Auth, User, Role, Menu）
```

## 快速启动

```bash
# 1. 启动数据库
make env-up

# 2. 执行建表 SQL（按顺序）
# sql/init_rbac.sql
# sql/sys_notice.sql
# sql/sys_dict.sql
# sql/sys_file.sql
# sql/sys_oper_log.sql

# 3. 启动后端
make dev-api

# 4. 访问接口文档
# http://localhost:8080/doc.html

# 5. 默认账号
# 用户名: admin
# 密码: admin123
```

## 核心功能一览

| 功能 | 注解/接口 | 说明 |
| :--- | :--- | :--- |
| 统一返回体 | `Result<T>` | code/msg/data 三字段 |
| 全局异常 | `@RestControllerAdvice` | 永不返回 Tomcat 报错页 |
| 登录认证 | `POST /api/auth/v1/login` | JWT Token |
| 权限校验 | `@RequirePermission("xxx")` | AOP 拦截 |
| 匿名访问 | `@PassAuth` | 跳过 Token 校验 |
| 操作日志 | `@OperLog(title="", businessType=1)` | AOP 自动记录 |
| 接口限流 | `@RateLimit(limit=5, window=60)` | Redis + Lua |
| 防重复提交 | `@RepeatSubmit(interval=3)` | Redis SETNX |
| 文件上传 | `POST /api/system/v1/files/upload` | 本地存储 |
| 在线用户 | `GET /api/system/v1/monitor/online/list` | Redis 扫描 |

## 文档目录

详细开发文档位于 `backend/docs/` 目录，按章节顺序阅读：

```
docs/
├── 00-接口规范/          # RESTful 规范、统一返回体
├── 01-基础环境/          # 环境搭建
├── 02-核心底座/          # Result, JWT, 异常处理
├── 03-权限插件/          # RBAC 集成
├── 04-RBAC权限体系/      # 五表设计、用户/角色/菜单
├── 05-系统功能/          # 字典、文件、通知、日志、在线用户
├── 06-高级特性/          # 接口限流、防重复提交
├── 07-部署运维/          # 打包、Docker
└── 08-前后端对接/         # 前端开发指南

frontend_admin/docs/     # 17 章前端文档（管理后台 Element Plus）
frontend_user/docs/      # 10 章前端文档（用户前台 Tailwind CSS）
```

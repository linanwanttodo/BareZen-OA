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
| 考勤打卡 | 100% | 上下班打卡 + 迟到/早退计算 + 月度统计 |
| 多级审批引擎 | 100% | 模板驱动节点链（leave/overtime/travel/vehicle） |
| 待办中心 | 100% | 按当前审批节点聚合 + WebSocket 通知 |
| 资产分类/借用归还 | 100% | 分类树 + 借用状态流转 |
| 知识库分类/文档版本 | 100% | 分类树 + 更新前版本快照 |
| 开放平台 | 100% | API Key 鉴权 + Webhook 事件转发 |
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

# 2. 启动后端（数据库结构由 Flyway 自动迁移，无需手动执行 SQL）
make dev-api

# 3. 访问接口文档
# http://localhost:8080/doc.html

# 4. 默认账号
# 用户名: admin
# 密码: admin123
```

> 数据库结构变更：新增 `bz_web_app/src/main/resources/db/migration/V{编号}__*.sql`，启动时自动应用；V1-V24 历史脚本不可修改。

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

frontend_admin/docs/     # 前端开发文档（管理后台 Arco Design，00-17 章）
frontend_user/docs/      # 前端开发文档（用户前台 Tailwind CSS）
```

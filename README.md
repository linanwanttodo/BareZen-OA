# BareZen_OA — 极简模块化全栈开发骨架

**BareZen_OA** 是一个面向现代个人开发者和小型团队的全栈开发骨架，基于 Java 21 + Spring Boot 3.2 + Vue 3.4 + TypeScript。

## 核心特性

- **微内核架构**：核心底座不绑定任何业务表结构，企业权限作为"插件"按需加载
- **纯净 JWT 鉴权**：告别 Spring Security 的臃肿，自定义 JWT 拦截器，代码透明可控
- **一后带两前**：管理后台（Vue 3 + TDesign）+ 用户前台（Vue 3 + Tailwind CSS）
- **Java 21 虚拟线程**：充分利用现代 JDK 的 I/O 性能优势
- **Gradle Kotlin DSL**：现代化的构建配置

## 技术栈

| 层 | 技术 |
|:---|:---|
| 后端 | Spring Boot 3.2.4 / Java 21 / MyBatis-Plus 3.5.7 |
| 安全 | JWT (jjwt 0.12) / BCrypt / AOP 权限注解 |
| 缓存 | Redis 7.0 |
| 数据库 | MySQL 8.0 |
| 文档 | Knife4j (`/doc.html`) |
| 管理端 | Vue 3.4 / TDesign / Pinia / ECharts |
| 用户端 | Vue 3.4 / Tailwind CSS + TDesign / Pinia |
| 构建 | Gradle 8+ / Vite 5 |
| 部署 | Docker / Docker Compose |

## 快速开始

### 1. 启动基础设施

```bash
cd backend
make env-up        # 启动 MySQL + Redis Docker 容器
```

### 2. 初始化数据库（Flyway 自动迁移）

数据库结构由 **Flyway 自动迁移**管理，无需手动执行 SQL 脚本。首次启动后端时会自动创建 `barezen_oa` 库表并写入种子数据（V1-V47）：

```bash
# 只需确保 MySQL 存在 barezen_oa 数据库（或由连接配置自动创建）
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS barezen_oa CHARACTER SET utf8mb4;"
```

后续结构变更一律新增 `backend/bz_web_app/src/main/resources/db/migration/V{编号}__*.sql`，启动时按版本号顺序自动应用，历史脚本不可修改。

### 3. 启动后端

```bash
cd backend
make dev-api       # 默认启动在 http://localhost:8080
```

### 4. 启动前端

```bash
# 管理后台（端口 5173）
cd frontend_admin
npm install
npm run dev

# 用户前台（端口 5174）
cd frontend_user
npm install
npm run dev
```

### 5. 默认账号

- 用户名：`admin`
- 密码：`admin123`

> **注意**：`admin/admin123` 仅用于本地开发环境。该弱口令不满足生产强密码策略
> （8 位+大小写+数字+特殊字符，见 `PasswordUtil.validatePasswordPolicy`），
> **生产部署后必须立即改密**，禁止将默认账号用于任何对外环境。

## 项目结构

```
BareZen_OA/
├── backend/                  # 后端 Gradle 多模块
│   ├── bz_framework_core/    # 核心底座 + RBAC + 业务域（hr/office/asset/kb/system）
│   ├── bz_web_app/           # Web 启动模块
│   │   └── src/main/resources/db/migration/  # Flyway 迁移脚本（V1-V47）
│   ├── docker-compose.yml    # MySQL + Redis
│   └── Dockerfile            # 后端容器化
├── frontend_admin/           # 管理后台（Vue 3 + TDesign）
├── frontend_user/            # 用户前台（Vue 3 + Tailwind CSS）
├── design-system/            # 设计规范文档
└── docs/                     # 项目文档（.md）
```

## 模块功能

| 功能 | 后端 | 管理端 | 用户端 |
|:---|:---|:---|:---|
| 用户/角色/菜单 CRUD | 是 | 是 | — |
| JWT 登录/登出 | 是 | 是 | 是 |
| 验证码 | 是 | 是 | 是 |
| 数据字典 | 是 | 是 | — |
| 文件上传/下载 | 是 | 是 | 是 |
| 通知公告 | 是 | 是 | 是 |
| 操作日志 | 是 | 是 | — |
| 在线用户监控 | 是 | 是 | — |
| 员工通讯录 | 是 | — | 是 |
| 考勤打卡（上下班/迟到早退统计） | 是 | 是 | 是 |
| 请假/加班/出差/用车申请 | 是 | 是 | 是 |
| 多级审批引擎（模板配置 + 节点链） | 是 | 是 | — |
| 待办中心 | 是 | 是 | 是 |
| 会议预订（行锁防冲突） | 是 | 是 | 是 |
| 办公用品申领/库存预警 | 是 | 是 | 是 |
| 资产台账 + 分类 + 借用归还 | 是 | 是 | — |
| 知识库 + 分类 + 文档版本 | 是 | 是 | — |
| 日程管理 | 是 | 是 | 是 |
| 开放平台（API Key / Webhook） | 是 | 是 | — |
| 仪表盘 | 是 | 是 | 是 |
| 接口限流 | 是 | — | — |
| 防重复提交 | 是 | — | — |

## 环境变量

参考各目录下的 `.env.example`。

### 后端关键变量

| 变量 | 默认值 | 说明 |
|:---|:---|:---|
| `DB_PASSWORD` | 123456 | MySQL 密码 |
| `JWT_SECRET` | 开发默认 | 生产用 `openssl rand -base64 32` |
| `AUTH_REGISTER_ENABLED` | false | 开放注册开关 |
| `CORS_ALLOWED_ORIGINS` | (空=放行所有) | 生产配置具体域名 |

## 安全注意事项

1. **JWT 密钥**：生产环境务必通过 `JWT_SECRET` 环境变量设置随机密钥
2. **数据库密码**：生产环境禁用默认密码
3. **CORS**：生产环境设置 `CORS_ALLOWED_ORIGINS` 限定具体域名
4. **注册**：生产建议关闭开放注册（默认关闭）
5. **文件下载**：管理员私有文件需登录才能访问

## 许可

MIT

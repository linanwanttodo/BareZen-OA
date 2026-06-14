# BareZen_OA 前端项目总览

## 概览

本项目包含两个独立的前端工程，共同对接后端 `backend/`：

```
BareZen_OA/
├── frontend_admin/            # 管理后台 (Vue 3 + Arco Design)
│   ├── docs/                 # 开发文档 (本目录)
│   └── src/
│       ├── api/              # 对接 /api/system/* + /api/auth/*
│       ├── layout/           # 侧边栏 + 顶栏布局
│       ├── router/           # Vue Router 路由
│       ├── stores/           # Pinia 状态管理
│       ├── views/            # 业务页面
│       └── utils/            # 工具函数
│
└── frontend_user/            # 用户前台 (Vue 3 + Tailwind CSS)
    ├── docs/                 # 开发文档 (本目录)
    └── src/
        ├── api/              # 对接 /api/app/*
        ├── pages/            # 页面级组件
        ├── router/           # Vue Router 路由
        ├── stores/           # Pinia 状态管理
        └── utils/            # 工具函数
```

## 文档阅读顺序

### frontend_admin 文档顺序

```
docs/
├── 00-项目规范/              # 项目介绍、技术栈、目录结构
├── 00.5-设计系统/           # UI/UX Pro Max 设计规范
├── 01-环境搭建/              # Node 22 + pnpm + Vite + Vue 3 + TypeScript
├── 02-项目初始化/             # 目录结构 + 配置文件 + 代码规范
├── 03-路由与布局/             # Vue Router + 管理后台 Layout
├── 04-状态管理/               # Pinia stores (user, auth, settings)
├── 05-请求封装/               # Axios 实例 + 拦截器 + 统一错误处理
├── 06-权限管理/               # 登录 + Token 存储 + 路由守卫 + 按钮权限
├── 07-组件开发规范/           # 组件规范 + Hooks + Composable
├── 08-用户管理模块/            # 用户列表 + 新增 + 编辑 + 删除
├── 09-角色管理模块/            # 角色列表 + 权限分配
├── 10-菜单管理模块/            # 菜单树 + 动态路由
├── 11-字典管理模块/            # 字典类型 + 字典数据
├── 12-文件管理模块/            # 文件上传 + 列表 + 下载
├── 13-通知公告模块/            # 公告列表 + 发布 + 下架
├── 14-操作日志模块/            # 日志列表 + 搜索 + 导出
├── 15-在线用户模块/            # 在线用户列表 + 强制下线
├── 16-打包部署/               # Vite build + Nginx 配置
├── 17-前后端对接/             # API 对接细节 + Mock 数据
```

### frontend_user 文档顺序

```
docs/
├── 00-项目规范/              # 项目介绍、技术栈、目录结构
├── 01-环境搭建/              # Node 22 + pnpm + Vite + Vue 3 + Tailwind CSS
├── 02-项目初始化/             # 目录结构 + 配置文件 + 代码规范
├── 03-路由与布局/             # Vue Router + 公共布局 (Header/Footer)
├── 04-状态管理/               # Pinia stores (user, cart, order)
├── 05-请求封装/               # Axios 实例 + 拦截器
├── 06-认证模块/               # 注册 + 登录 + 找回密码 + JWT 存储
├── 07-首页开发/               # Banner + 分类 + 热门内容
├── 08-个人中心/               # 个人信息 + 我的申请 + 考勤打卡
├── 09-打包部署/               # Vite build + Nginx 配置
└── 10-前后端对接/             # API 对接细节
```

## 技术栈对照表

| 维度 | frontend_admin | frontend_user |
|:---|:---|:---|
| 构建工具 | Vite 5.x | Vite 5.x |
| 核心框架 | Vue 3.4 + Composition API | Vue 3.4 + Composition API |
| 类型系统 | TypeScript 5.x | TypeScript 5.x |
| UI 组件库 | Arco Design (管理端) | Tailwind CSS (用户端) |
| 状态管理 | Pinia 2.x | Pinia 2.x |
| 路由 | Vue Router 4.x | Vue Router 4.x |
| HTTP 客户端 | Axios | Axios |
| CSS 方案 | Scoped CSS + Arco Design 样式覆盖 | Tailwind CSS 原子化 |
| 图标 | @arco-design/web-vue/icon | @iconify/vue (Lucide) |
| 构建目标 | PC 浏览器 (≥ IE11 无需支持) | PC + 移动端响应式 |

## 后端接口对接

后端 `backend/` 提供以下接口前缀供前端对接：

| 前缀 | 模块 | 说明 |
|:---|:---|:---|
| `/api/auth/v1/*` | 认证 | 登录/注册/验证码/登出 |
| `/api/system/v1/users/*` | 用户管理 | CRUD |
| `/api/system/v1/roles/*` | 角色管理 | CRUD + 菜单分配 |
| `/api/system/v1/menus/*` | 菜单管理 | 路由/树形 |
| `/api/system/v1/dict/*` | 字典管理 | 类型/数据 |
| `/api/system/v1/files/*` | 文件管理 | 上传/下载/删除 |
| `/api/system/v1/notices/*` | 通知公告 | CRUD + 发布/下架 |
| `/api/system/v1/oper-logs/*` | 操作日志 | 分页查询 |
| `/api/system/v1/monitor/online/*` | 在线用户 | 列表/强制下线 |
| `/api/common/v1/health` | 健康检查 | 服务状态 |

## 环境要求

| 环境 | 版本要求 |
|:---|:---|
| Node.js | ≥ 20.0.0 (推荐 22.x LTS) |
| pnpm | ≥ 9.0.0 |
| 浏览器 | Chrome 120+ / Firefox 120+ / Safari 17+ |

## 快速开始

### 管理后台 (frontend_admin)

```bash
cd frontend_admin
pnpm install
pnpm dev          # http://localhost:5173
pnpm build        # 打包
pnpm preview      # 预览打包结果
```

### 用户前台 (frontend_user)

```bash
cd frontend_user
pnpm install
pnpm dev          # http://localhost:5174
pnpm build        # 打包
pnpm preview      # 预览打包结果
```

## 项目负责人提示

> 文档按章节顺序编写，每章都会：
> 1. 解释**为什么**这样做（背景和原理）
> 2. 展示**完整的**代码和注释（逐行解析）
> 3. 提供**可运行**的代码（复制即可使用）
> 4. 包含**步骤清单**（一步一步跟着做）
>
> 如果某章节的代码依赖其他章节的内容，会在开头注明"前置要求"。

# BareZen_Base 用户前台开发文档

## 文档阅读顺序

```
docs/
├── 00-项目规范/              # 项目介绍、技术栈、目录结构
├── 01-环境搭建/              # Node 22 + pnpm + Vite + Vue 3 + Tailwind CSS
├── 02-项目初始化/             # 目录结构 + 配置文件 + Tailwind CSS 配置
├── 03-路由与布局/             # Vue Router + 公共布局 (Header/Footer/Sidebar)
├── 04-状态管理/               # Pinia stores (user, auth, cart, order)
├── 05-请求封装/               # Axios 实例 + 拦截器
├── 06-认证模块/               # 注册 + 登录 + 找回密码 + JWT 存储
├── 07-首页开发/               # Banner + 分类 + 热门内容
├── 08-内容详情页/             # 详情组件 + 评论 + 相关推荐
├── 09-个人中心/               # 个人信息 + 我的收藏 + 历史记录
├── 10-打包部署/               # Vite build + Nginx 配置
└── 11-前后端对接/             # API 对接细节
```

## 技术栈对照表

| 技术 | 版本 | 说明 |
|:---|:---|:---|
| 构建工具 | Vite 5.x | 前端构建 |
| 核心框架 | Vue 3.4 + Composition API | 框架 |
| TypeScript | 5.x | 类型系统 |
| UI 方案 | Tailwind CSS 3.x | 原子化 CSS |
| 状态管理 | Pinia 2.x | 状态管理 |
| 路由 | Vue Router 4.x | 路由 |
| HTTP | Axios 1.x | 请求 |
| 图标 | @iconify/vue (Lucide) | 图标库 |
| 构建目标 | PC + 移动端响应式 | 多端适配 |

## 快速开始

```bash
cd frontend_user
pnpm install
pnpm dev          # http://localhost:5174
pnpm build
```

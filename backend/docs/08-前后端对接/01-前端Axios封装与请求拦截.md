# 01-前端Axios封装与请求拦截

## 1. 功能概述

前端通过 Axios 发送 HTTP 请求到后端 API。本章介绍前端如何封装 Axios，统一处理 Token 注入、错误响应和登录跳转。

---

## 2. 步骤一：创建 request.js

### 2.1 request.js 完整代码

文件路径：`frontend_admin/src/utils/request.js`

```javascript
import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import router from '@/router'

// 创建 Axios 实例
const service = axios.create({
  // baseURL：请求前缀，从环境变量读取，默认 /api
  // Vite 项目使用 import.meta.env 读取 .env 文件中的变量
  baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
  timeout: 30000,  // 请求超时时间（30秒）
  headers: { 'Content-Type': 'application/json' }
})

// ============================================
// 请求拦截器
// ============================================
// 作用：在每次发送请求前自动注入 Token
service.interceptors.request.use(
  config => {
    // 从 localStorage 中读取 Token（用户登录后由后端返回并存储在此）
    const token = localStorage.getItem('token')

    // 如果 Token 存在，则注入到请求头中
    // 后端 AuthInterceptor 会从 Authorization 头中提取 Token
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }

    return config
  },
  error => {
    // 请求发送失败（如网络断开），直接拒绝 Promise
    return Promise.reject(error)
  }
)

// ============================================
// 响应拦截器
// ============================================
// 作用：统一处理后端返回的结果，根据 code 值决定行为
service.interceptors.response.use(
  response => {
    const res = response.data

    // code === 200 表示操作成功，直接返回 data
    if (res.code === 200) {
      return res
    }

    // code === 401 表示 Token 过期或未登录
    if (res.code === 401) {
      localStorage.removeItem('token')
      ElMessage.error(res.msg || '登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(new Error(res.msg || '登录已过期'))
    }

    // code === 403 表示没有权限
    if (res.code === 403) {
      ElMessage.error(res.msg || '没有访问权限')
      return Promise.reject(new Error(res.msg || '没有访问权限'))
    }

    // 其他错误码（400、500等）
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  error => {
    // HTTP 层面的错误（网络断开、服务未启动等）
    ElNotification({
      title: '网络错误',
      message: '服务器异常，请检查网络连接',
      type: 'error',
      duration: 5000
    })
    return Promise.reject(error)
  }
)

export default service
```

**逐行讲解**：

- `axios.create({ baseURL: '...' })`：创建 Axios 实例并设置基础 URL。所有通过此实例发出的请求都会自动拼接 baseURL，无需在每个请求中写完整路径。

- `interceptors.request.use()`：请求拦截器，在所有请求发送前执行。在这里从 localStorage 读取 Token 并注入到 Authorization 请求头中。

- `interceptors.response.use()`：响应拦截器，在所有响应到达后执行。统一处理后端返回的 code 码，200=成功，401=跳转登录，403=提示无权限。

- `router.push('/login')`：当 Token 过期时自动跳转到登录页面，用户体验流畅，无需手动操作。

---

## 3. 步骤二：创建 API 调用模块

### 3.1 user.js — 用户相关 API

文件路径：`frontend_admin/src/api/system/user.js`

```javascript
import request from '@/utils/request'

// 分页查询用户列表
export function listUsers(params) {
  return request({
    url: '/system/v1/users/list',
    method: 'get',
    params  // { pageNum: 1, pageSize: 10, userName: 'admin' }
  })
}

// 根据ID查询用户
export function getUserById(id) {
  return request({
    url: `/system/v1/users/${id}`,
    method: 'get'
  })
}

// 新增用户
export function addUser(data) {
  return request({
    url: '/system/v1/users',
    method: 'post',
    data
  })
}

// 更新用户
export function updateUser(data) {
  return request({
    url: '/system/v1/users',
    method: 'put',
    data
  })
}

// 删除用户
export function delUser(id) {
  return request({
    url: `/system/v1/users/${id}`,
    method: 'delete'
  })
}
```

### 3.2 auth.js — 登录认证 API

文件路径：`frontend_admin/src/api/auth.js`

```javascript
import request from '@/utils/request'

// 登录（不需要 Token）
export function login(data) {
  return request({
    url: '/auth/v1/login',
    method: 'post',
    data  // { username: 'admin', password: 'admin123' }
  })
}

// 获取当前用户信息（需要 Token）
export function getCurrentUserInfo() {
  return request({
    url: '/system/v1/users/current/info',
    method: 'get'
  })
}
```

---

## 4. 步骤三：配置 Vite 环境变量

### 4.1 .env.development（开发环境）

文件路径：`frontend_admin/.env.development`

```env
# 开发环境 API 基础地址
VITE_APP_BASE_API = '/api'
```

### 4.2 .env.production（生产环境）

文件路径：`frontend_admin/.env.production`

```env
# 生产环境 API 基础地址
VITE_APP_BASE_API = 'https://api.yourdomain.com'
```

---

## 5. 步骤四：配置 Vite 代理

### 5.1 vite.config.js

文件路径：`frontend_admin/vite.config.js`

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // 将 /api 开头的请求代理到后端服务器
      '/api': {
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true,
        // 不需要重写路径，后端接口本身就以 /api 开头
        // rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

**为什么需要代理？**
前端开发时运行在 `localhost:3000`，后端运行在 `localhost:8080`。直接跨域请求会被浏览器拦截。通过 Vite 的 proxy 配置，开发服务器将 /api 开头的请求转发到后端，绕过跨域限制。

---

## 6. 完整请求流程

```text
前端 Vue 页面
  -> 调用 @/api/system/user.js 中的 listUsers()
    -> 调用 @/utils/request.js 中的 service 实例
      -> 请求拦截器：注入 Authorization Header
      -> 发送 HTTP 请求
      -> Vite 代理转发到 http://localhost:8080/api/...
        -> 后端 AuthInterceptor 鉴权
        -> 后端 Controller 处理
        -> 返回 Result 对象
      -> 响应拦截器：根据 code 处理
        -> code=200: 返回 data
        -> code=401: 跳转登录页
        -> 其他: 提示错误
```

---

## 7. 本章完成检查清单

- [ ] `request.js` 已创建，包含请求拦截器和响应拦截器
- [ ] 请求拦截器自动注入 Authorization Header
- [ ] 响应拦截器处理 401（跳转登录）和 403（提示无权限）
- [ ] 各模块 API 文件已创建（user.js, auth.js）
- [ ] Vite 代理配置已添加，解决跨域问题

---

*下一章：[02-登录流程前后端完整对接](./02-登录流程前后端完整对接.md)*
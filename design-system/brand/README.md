# BareZen 品牌 Logo 源文件

> 本目录存放 Logo **设计源文件（原图，请勿直接用于页面）**。
> 页面实际引用的是各前端 `public/` 下的压缩产物。

## 源文件

| 文件 | 说明 | 用途 |
|------|------|------|
| `logo-transparent.png` | 透明背景版（1004×1004 RGBA，蓝色图形，白色部分为透明） | 网站 Logo 源图：导航栏、favicon、登录页品牌头 |
| `logo-on-white.png` | 白底版（1254×1254 RGB） | 管理端登录页左侧配图源图 |

## 派生产物（代码实际引用）

| 产物 | 来源 | 位置 |
|------|------|------|
| `logo.png`（256×256，已裁透明留白并居中） | `logo-transparent.png` | `frontend_admin/public/`、`frontend_user/public/` |
| `login-visual.png`（800×800） | `logo-on-white.png` | `frontend_admin/public/` |

## 使用约定

- 透明版白色区域是**负空间**（"禅"字白圆、拼图缝、腿间月牙与外部连通），只适合放在白色/浅灰（`--wecom-bg-page`）背景上；深色背景需先垫白色底板。
- 更换 Logo 时：替换本目录源文件 → 重新导出 `public/` 派生产物 → 双端构建验证。
- 旧的 `logo.svg` / `logo-icon.svg`（"B" 方块占位）已不再被引用，确认无误后可删除。

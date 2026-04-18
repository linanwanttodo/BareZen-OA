# Design System Master File — BareZenUser

> **使用逻辑**：构建具体页面时，先检查 `design-system/pages/[页面名].md`。若存在，其规则**覆盖**本文件；否则严格遵循本文件。

---

**项目**：BareZenUser
**生成时间**：2026-05-08 10:36:49
**分类**：服务落地页（Service Landing Page）
**风格**：企业级扁平（无渐变 / 无悬浮位移 / 无霓虹发光 / 无 Emoji）

## 1.1 全局规范

### 配色方案

| 角色 | Hex | CSS 变量 | 说明 |
|:---|:---|:---|:---|
| Primary | `#0EA5E9` | `--color-primary` | 主色（天蓝），用于品牌、主操作 |
| Secondary | `#38BDF8` | `--color-secondary` | 次色（浅蓝），用于次要元素 |
| CTA / Accent | `#F97316` | `--color-cta` | 强调/操作色（橙），用于 CTA 按钮 |
| Background | `#F0F9FF` | `--color-background` | 页面背景（极浅蓝） |
| Text | `#0C4A6E` | `--color-text` | 正文文字（深蓝） |

**配色说明**：天蓝信任色 + 暖橙 CTA，友好专业，适用于服务门户与落地页。

### 字体规范

- **标题字体**：Plus Jakarta Sans
- **正文字体**：Plus Jakarta Sans
- **风格**：friendly, modern, saas, clean, approachable, professional
- **Google Fonts**：[Plus Jakarta Sans](https://fonts.google.com/share?selection.family=Plus+Jakarta+Sans:wght@300;400;500;600;700)

**CSS 引入**：
```css
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');
```

### 间距规范

| Token | 值 | 用途 |
|:---|:---|:---|
| `--space-xs` | 4px / 0.25rem | 紧凑间距 |
| `--space-sm` | 8px / 0.5rem | 图标间距、行内间距 |
| `--space-md` | 16px / 1rem | 标准内边距 |
| `--space-lg` | 24px / 1.5rem | 区块内边距 |
| `--space-xl` | 32px / 2rem | 大间距 |
| `--space-2xl` | 48px / 3rem | 区块外边距 |
| `--space-3xl` | 64px / 4rem | 主视觉内边距 |

### 阴影层级

| 层级 | 值 | 用途 |
|:---|:---|:---|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | 轻微抬升 |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.1)` | 卡片、按钮 |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.1)` | 弹窗、下拉 |
| `--shadow-xl` | `0 20px 25px rgba(0,0,0,0.15)` | 主视觉、特色卡片 |

## 1.2 组件规范

### 按钮 (Buttons)

```css
/* 主要按钮 */
.btn-primary {
  background: #F97316;
  color: #FFFFFF;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:active {
  opacity: 0.9;
}

/* 次要按钮 */
.btn-secondary {
  background: transparent;
  color: #0EA5E9;
  border: 2px solid #0EA5E9;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
```

### 卡片 (Cards)

```css
.card {
  background: #F0F9FF;
  border: 1px solid #E0F2FE;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
}
```

> **注意**：企业扁平风格不使用悬浮位移（`translateY` / `scale`）。状态变化仅通过边框或背景色表达，避免布局偏移。

### 输入框 (Inputs)

```css
.input {
  padding: 12px 16px;
  border: 1px solid #BAE6FD;
  border-radius: 8px;
  font-size: 16px;
  color: #0C4A6E;
  background: #FFFFFF;
}

.input:focus {
  border-color: #0EA5E9;
  outline: 2px solid #0EA5E9;
  outline-offset: 0;
}
```

### 弹窗 (Modals)

```css
.modal-overlay {
  background: rgba(0, 0, 0, 0.5);
}

.modal {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-xl);
  max-width: 500px;
  width: 90%;
}
```

## 1.3 风格指南

**风格**：Enterprise Flat（企业级扁平）

**关键词**：干净、简洁、留白、功能化、高对比、几何、无衬线、网格化

**适用**：企业应用、服务门户、文档站点、SaaS 平台、专业工具

**关键效果**：状态变化通过边框色与背景色表达，无位移、无发光、无渐变

### 页面模式

**模式名称**：Minimal Single Column（极简单栏）

- **CTA 位置**：居中 / 主操作栏
- **区块顺序**：1. 主标题 2. 简短描述 3. 要点列表 4. CTA 5. 页脚

## 1.4 反模式（禁止使用）

- 渐变填充（线性/径向）
- 悬浮位移（translateY / scale）
- 霓虹发光 / 文字阴影 / 发光阴影
- Emoji 作为图标或列表标记
- 装饰性模糊（backdrop-filter: blur）或半透明遮罩

### 额外禁止项

| 禁止 | 说明 |
|:---|:---|
| Emoji 作为图标 | 使用 SVG 图标（Heroicons / Lucide / Simple Icons） |
| 布局偏移的悬浮 | 避免导致位移的 transform |
| 低对比度文字 | 正文对比度 ≥ 4.5:1 |
| 不可见的 Focus 状态 | 键盘导航 focus 必须可见 |

## 1.5 交付前检查清单

- [ ] 无 Emoji 作为图标（使用 SVG）
- [ ] 图标来自统一图标集（Heroicons / Lucide）
- [ ] 可点击元素有 `cursor: pointer`
- [ ] 无悬浮位移 / transform 动画
- [ ] 无渐变或发光效果
- [ ] Light 模式文字对比度 ≥ 4.5:1
- [ ] Focus 状态可见（键盘导航）
- [ ] 尊重 `prefers-reduced-motion`
- [ ] 响应式：375 / 768 / 1024 / 1440
- [ ] 内容不被固定导航遮挡
- [ ] 移动端无水平滚动

# Design System Master File — BareZenEdu

> **使用逻辑**：构建具体页面时，先检查 `design-system/pages/[页面名].md`。若存在，其规则**覆盖**本文件；否则严格遵循本文件。

---

**项目**：BareZenEdu
**生成时间**：2026-05-08 09:09:46
**分类**：教育类应用（Educational App）
**风格**：企业级扁平（无渐变 / 无悬浮位移 / 无霓虹发光 / 无 Emoji）

## 1.1 全局规范

### 配色方案

| 角色 | Hex | CSS 变量 | 说明 |
|:---|:---|:---|:---|
| Primary | `#4F46E5` | `--color-primary` | 主色（靛蓝），用于品牌、关键操作 |
| Secondary | `#818CF8` | `--color-secondary` | 次色（浅靛蓝），用于次要元素 |
| CTA / Accent | `#22C55E` | `--color-cta` | 强调/操作色（绿），用于进度、主要按钮 |
| Background | `#EEF2FF` | `--color-background` | 页面背景（极浅靛蓝） |
| Text | `#312E81` | `--color-text` | 正文文字（深靛蓝） |

**配色说明**：靛蓝品牌色 + 绿色进度指示，沉稳专业，适用于教育管理后台。

### 字体规范

- **标题字体**：Inter
- **正文字体**：Inter
- **风格**：enterprise, calm, legible, trustworthy
- **Google Fonts**：[Inter](https://fonts.google.com/share?selection.family=Inter:wght@400;500;600;700)

**CSS 引入**：
```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
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
  background: #22C55E;
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
  color: #4F46E5;
  border: 2px solid #4F46E5;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
```

### 卡片 (Cards)

```css
.card {
  background: #EEF2FF;
  border: 1px solid #E0E7FF;
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
  border: 1px solid #C7D2FE;
  border-radius: 8px;
  font-size: 16px;
  color: #312E81;
  background: #FFFFFF;
}

.input:focus {
  border-color: #4F46E5;
  outline: 2px solid #4F46E5;
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

**关键词**：中性色板、1px 边框、纯色品牌色、清晰层级、无装饰、高可读性

**适用**：LMS、教育管理、课程管理、企业培训门户

**关键效果**：状态变化通过边框色与背景色表达，无位移、无发光、无渐变

### 页面模式

**模式名称**：Structured Navigation + Content（结构化导航 + 内容）

- **CTA 位置**：工具栏 / 主操作栏
- **区块顺序**：页头 > 侧边导航 > 内容列表 > 详情视图

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

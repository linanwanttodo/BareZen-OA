#!/usr/bin/env node
/**
 * Arco → TDesign 全量机械映射脚本（P2/P3 批次）
 *
 * 覆盖：组件标签改名、属性级映射（button/tag/radio/modal/popconfirm）、
 * script 侧 Message/Modal/Notification 别名化、表格事件回填（从 git HEAD 提取）。
 * 非机械点（tree-select 数据字段、date-picker 返回格式、confirm-loading、
 * a-statistic、a-input-search、插槽撞名）由人工清单核对。
 *
 * 用法：node scripts/fix-arco-leftovers.mjs <file...>
 */
import fs from 'node:fs'
import { execSync } from 'node:child_process'

// ---- 组件改名映射（自闭合与配对标签统一处理）----
const RENAME = {
  'a-button': 't-button',
  'a-form-item': 't-form-item',
  'a-form': 't-form',
  'a-input': 't-input', // 注意：须在 a-input-password / a-input-number / a-input-search 之前处理（用长名优先排序）
  'a-input-password': 't-input',
  'a-input-number': 't-input-number',
  'a-input-search': 't-input',
  'a-textarea': 't-textarea',
  'a-select': 't-select',
  'a-option': 't-option',
  'a-optgroup': 't-option-group',
  'a-radio-group': 't-radio-group',
  'a-radio': 't-radio',
  'a-checkbox-group': 't-checkbox-group',
  'a-checkbox': 't-checkbox',
  'a-switch': 't-switch',
  'a-date-picker': 't-date-picker',
  'a-range-picker': 't-date-range-picker',
  'a-modal': 't-dialog',
  'a-drawer': 't-drawer',
  'a-popconfirm': 't-popconfirm',
  'a-tag': 't-tag',
  'a-card': 't-card',
  'a-tabs': 't-tabs',
  'a-tab-pane': 't-tab-panel',
  'a-pagination': 't-pagination',
  'a-spin': 't-loading',
  'a-empty': 't-empty',
  'a-alert': 't-alert',
  'a-avatar': 't-avatar',
  'a-badge': 't-badge',
  'a-steps': 't-steps',
  'a-step': 't-step-item',
  'a-descriptions-item': 't-descriptions-item',
  'a-descriptions': 't-descriptions',
  'a-tree-select': 't-tree-select',
  'a-dropdown': 't-dropdown',
  'a-doption': 't-dropdown-item',
  'a-space': 't-space',
  'a-row': 't-row',
  'a-col': 't-col',
  'a-divider': 't-divider',
  'a-link': 't-link',
}
// 长名优先，避免 a-input 吃掉 a-input-password
const renameKeys = Object.keys(RENAME).sort((a, b) => b.length - a.length)

const TAG_COLOR = {
  arcoblue: 'theme="primary"',
  red: 'theme="danger"',
  green: 'theme="success"',
  orange: 'theme="warning"',
  purple: 'theme="warning"',
  gold: 'theme="warning"',
  gray: 'theme="default"',
  cyan: 'theme="primary"',
}

function convertTag(name, attrs) {
  let out = ''
  // button 属性映射
  if (name === 't-button') {
    attrs = attrs
      .replace(/type="text"/g, 'variant="text"')
      .replace(/type="primary"/g, 'theme="primary"')
      .replace(/type="outline"/g, 'variant="outline"')
      .replace(/type="dashed"/g, 'variant="dashed"')
      .replace(/status="danger"/g, 'theme="danger"')
      .replace(/status="warning"/g, 'theme="warning"')
      .replace(/status="success"/g, 'theme="success"')
      .replace(/ status="normal"/g, '')
  }
  // input-password / input-search 补属性
  if (name === 't-input' && attrs.includes('__password__')) {
    attrs = attrs.replace('__password__', 'type="password"')
  }
  if (name === 't-radio-group') {
    attrs = attrs.replace(/type="button"/g, 'variant="default-filled"')
  }
  // dialog（原 modal）
  if (name === 't-dialog') {
    attrs = attrs
      .replace(/\bv-model:visible=/g, 'v-model=')
      .replace(/\s@ok=/g, ' @confirm=')
      .replace(/\stitle="/g, ' header="')
  }
  if (name === 't-drawer') {
    attrs = attrs.replace(/\bv-model:visible=/g, 'v-model=')
  }
  if (name === 't-popconfirm') {
    attrs = attrs.replace(/\s@ok=/g, ' @confirm=')
  }
  // tag 颜色映射 + 去 bordered
  if (name === 't-tag' || name === 't-tag') {
    attrs = attrs.replace(/color="(\w+)"/g, (m, c) => TAG_COLOR[c] || m)
    attrs = attrs.replace(/\sbordered\b/g, '')
  }
  // tree-select 数据字段：Arco 字段名 key/title/children → TDesign value/label/children
  if (name === 't-tree-select') {
    out += '\n<!-- TODO(tree-select): data 字段 key/title 需转为 value/label -->'
  }
  // date-picker 提醒核对返回格式
  if (name === 't-date-picker' || name === 't-date-range-picker') {
    out += '\n<!-- TODO(date-picker): 核对返回值格式（原 Arco 为字符串） -->'
  }
  out += `<${name}${attrs}>`
  return out
}

function processFile(file) {
  let src = fs.readFileSync(file, 'utf8')
  const notes = []

  // ---- 1. 组件标签改名 + 属性映射 ----
  // 打开标签（含自闭合与配对）
  for (const old of renameKeys) {
    const neu = RENAME[old]
    const extra = old === 'a-input-password' ? ' __password__' : ''
    // 开标签：<old ...> 或 <old .../>
    const openRe = new RegExp(`<${old}((?:\\s[^>]*)?)(/?)>`, 'g')
    src = src.replace(openRe, (m, attrs, selfClose) => {
      const conv = convertTag(neu, attrs)
      return conv.replace(/>$/, selfClose ? ' />' : '>')
    })
    // 闭标签
    const closeRe = new RegExp(`</${old}>`, 'g')
    src = src.replace(closeRe, `</${neu}>`)
  }

  // ---- 2. 表格事件回填：从 git HEAD 的 <a-table 标签上取 @change / @page-size-change ----
  let head = ''
  try {
    head = execSync(`git show HEAD:${file}`, { encoding: 'utf8', stdio: ['pipe', 'pipe', 'ignore'] })
  } catch {
    /* 新文件无历史 */
  }
  if (head) {
    const headTableRe = /<a-table((?:\s[^>]*)?)>/g
    let ht
    const events = []
    while ((ht = headTableRe.exec(head)) !== null) {
      const a = ht[1]
      const ev = a.match(/@change="([^"]+)"/)?.[1] || a.match(/@page-size-change="([^"]+)"/)?.[1]
      if (ev) events.push(ev)
    }
    if (events.length > 0) {
      let i = 0
      src = src.replace(/<t-table((?:\s[^>]*)?)>/g, (m, attrs) => {
        if (attrs.includes('@page-change')) return m
        const ev = events[i++] || events[0]
        return `<t-table${attrs} @page-change="${ev}">`
      })
      notes.push(`表格事件回填: ${events.join(', ')}`)
    }
  }

  // ---- 3. script 侧 API 别名化 ----
  src = src.replace(
    /import\s*{\s*Message\s*}\s*from\s*'@arco-design\/web-vue'/g,
    "import { MessagePlugin as Message } from 'tdesign-vue-next'",
  )
  src = src.replace(
    /import\s*{\s*Modal\s*}\s*from\s*'@arco-design\/web-vue'/g,
    "import { DialogPlugin as Modal } from 'tdesign-vue-next'",
  )
  src = src.replace(
    /import\s*{\s*Notification\s*}\s*from\s*'@arco-design\/web-vue'/g,
    "import { NotificationPlugin as Notification } from 'tdesign-vue-next'",
  )
  // 混合导入 import { Message, Modal } from '@arco-design/web-vue'
  src = src.replace(
    /import\s*{([^}]*)}\s*from\s*'@arco-design\/web-vue'/g,
    (m, names) => {
      const list = names.split(',').map((s) => s.trim()).filter(Boolean)
      const mapped = list.map((n) =>
        n === 'Message' ? 'MessagePlugin as Message'
        : n === 'Modal' ? 'DialogPlugin as Modal'
        : n === 'Notification' ? 'NotificationPlugin as Notification'
        : null,
      )
      if (mapped.every(Boolean)) {
        return `import { ${mapped.join(', ')} } from 'tdesign-vue-next'`
      }
      notes.push(`未识别的 arco 导入: ${names}`)
      return m
    },
  )

  fs.writeFileSync(file, src)
  return notes
}

// ---- 主流程 ----
const files = process.argv.slice(2)
const allNotes = []
for (const f of files) {
  const notes = processFile(f)
  if (notes.length) allNotes.push(`${f}: ${notes.join('; ')}`)
}
console.log(`processed ${files.length} files`)
if (allNotes.length) console.log('NOTES:\n' + allNotes.join('\n'))

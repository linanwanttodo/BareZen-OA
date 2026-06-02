#!/usr/bin/env node
/**
 * Arco → TDesign 机械替换脚本（P1 批次）
 *
 * 只处理“低风险、一一对应”的替换；以下情况留给手工：
 *  - a-table / a-table-column（表格列 → columns 数组，P2）
 *  - a-dropdown 内容插槽结构（#content → 直接子元素）
 *  - a-spin → t-loading 包装语义
 *  - FormInstance 等类型导入
 */
import fs from 'node:fs'
import path from 'node:path'

const TARGETS = process.argv.slice(2)

// 1:1 标签映射（开/闭标签）
const TAG_MAP = {
  'a-form': 't-form',
  'a-form-item': 't-form-item',
  'a-input': 't-input',
  'a-input-password': 't-input',
  'a-textarea': 't-textarea',
  'a-input-number': 't-input-number',
  'a-select': 't-select',
  'a-option': 't-option',
  'a-radio-group': 't-radio-group',
  'a-radio': 't-radio',
  'a-checkbox-group': 't-checkbox-group',
  'a-checkbox': 't-checkbox',
  'a-switch': 't-switch',
  'a-date-picker': 't-date-picker',
  'a-range-picker': 't-date-range-picker',
  'a-tree-select': 't-tree-select',
  'a-tabs': 't-tabs',
  'a-tab-pane': 't-tab-panel',
  'a-card': 't-card',
  'a-tag': 't-tag',
  'a-popconfirm': 't-popconfirm',
  'a-alert': 't-alert',
  'a-avatar': 't-avatar',
  'a-badge': 't-badge',
  'a-steps': 't-steps',
  'a-step': 't-step-item',
  'a-descriptions': 't-descriptions',
  'a-descriptions-item': 't-descriptions-item',
  'a-space': 't-space',
  'a-row': 't-row',
  'a-col': 't-col',
  'a-divider': 't-divider',
  'a-link': 't-link',
  'a-empty': 't-empty',
  'a-button': 't-button',
  'a-pagination': 't-pagination',
}

function replaceTags(src) {
  for (const [arco, td] of Object.entries(TAG_MAP)) {
    const arcoRe = new RegExp(arco.replace(/-/g, '\\-'), 'g')
    // 词边界：a-input 不误伤 a-input-password（先长后短排序已保证：password 先被映射）
  }
  return src
}

function migrateFile(file) {
  let src = fs.readFileSync(file, 'utf8')
  const before = src

  // 长标签优先（避免 a-input 吃掉 a-input-password）
  const pairs = Object.entries(TAG_MAP).sort((a, b) => b[0].length - a[0].length)
  for (const [arco, td] of pairs) {
    const re = new RegExp(`<(\\/?)${arco}(?=[\\s/>])`, 'g')
    src = src.replace(re, `<$1${td}`)
  }

  // a-button 属性迁移
  src = src.replace(/<t-button([^>]*)type="primary"/g, '<t-button$1theme="primary"')
  src = src.replace(/<t-button([^>]*)type="danger"/g, '<t-button$1theme="danger"')
  src = src.replace(/<t-button([^>]*)status="danger"/g, '<t-button$1theme="danger"')
  src = src.replace(/<t-button([^>]*)status="success"/g, '<t-button$1theme="success"')
  src = src.replace(/<t-button([^>]*)status="warning"/g, '<t-button$1theme="warning"')
  src = src.replace(/<t-button([^>]*)status="normal"/g, '<t-button$1theme="default"')
  src = src.replace(/<t-button([^>]*)\blong\b/g, '<t-button$1block')
  src = src.replace(/<t-button([^>]*)html-type=/g, '<t-button$1type=')
  src = src.replace(/<t-button([^>]*)allow-clear/g, '<t-button$1')

  // a-input allow-clear → clearable
  src = src.replace(/<t-input([^>]*)allow-clear/g, '<t-input$1clearable')

  // a-form-item field= → name=
  src = src.replace(/<t-form-item([^>]*)\bfield=/g, '<t-form-item$1name=')

  // a-tab-pane key= → value=
  src = src.replace(/<t-tab-panel([^>]*)\bkey=/g, '<t-tab-panel$1value=')

  // a-alert type= → theme=
  src = src.replace(/<t-alert([^>]*)\btype="(info|success|warning|error)"/g, '<t-alert$1theme="$2"')

  // a-modal → t-dialog 属性（标签已由 MAP 处理为 t-dialog? a-modal 未在 MAP 中，这里显式处理）
  src = src.replace(/<a-modal([^>]*)v-model:visible/g, '<t-dialog$1v-model')
  src = src.replace(/<a-modal/g, '<t-dialog')
  src = src.replace(/<\/a-modal>/g, '</t-dialog>')
  src = src.replace(/<t-dialog([^>]*)\btitle=/g, '<t-dialog$1header=')
  src = src.replace(/<t-dialog([^>]*)width="(\d+)"/g, '<t-dialog$1width="$2px"')
  src = src.replace(/<t-dialog([^>]*)width={/g, '<t-dialog$1width=')
  src = src.replace(/<t-dialog([^>]*)\b:width="/g, '<t-dialog$1width="')
  src = src.replace(/<t-dialog([^>]*):mask-closable="false"/g, '<t-dialog$1:close-on-overlay-click="false"')
  src = src.replace(/<t-dialog([^>]*)@ok=/g, '<t-dialog$1@confirm=')
  src = src.replace(/<t-dialog([^>]*)@before-ok=/g, '<t-dialog$1@confirm=')
  src = src.replace(/<t-dialog([^>]*)@cancel=/g, '<t-dialog$1@close=')
  src = src.replace(/<t-dialog([^>]*):confirm-loading=/g, '<t-dialog$1:confirm-btn="{ loading: ' )

  // a-popconfirm @ok → @confirm
  src = src.replace(/<t-popconfirm([^>]*)@ok=/g, '<t-popconfirm$1@confirm=')
  src = src.replace(/<t-popconfirm([^>]*)@before-ok=/g, '<t-popconfirm$1@confirm=')

  // a-doption → t-dropdown-item（结构手工核对）
  src = src.replace(/<a-doption/g, '<t-dropdown-item')
  src = src.replace(/<\/a-doption>/g, '</t-dropdown-item>')

  // 删除无效的 a-card-content 行（含闭合）
  src = src.replace(/^[ \t]*<a-card-content>.*\n/gm, '')
  src = src.replace(/^[ \t]*<\/a-card-content>.*\n/gm, '')

  // radio-group type="button" → variant="default-filled"
  src = src.replace(/<t-radio-group([^>]*)\btype="button"/g, '<t-radio-group$1variant="default-filled"')

  // script: Message → MessagePlugin
  src = src.replace(/import\s*{([^}]*?)\bMessage\b([^}]*?)}\s*from\s*'@arco-design\/web-vue'/g,
    (m, a, b) => `import {${a} MessagePlugin ${b}} from 'tdesign-vue-next'`)
  src = src.replace(/\bMessage\.(success|error|warning|info|loading)\(/g, 'MessagePlugin.$1(')

  // script: 删除本地图标导入（全局已注册同名 TDesign 图标）
  src = src.replace(/^import\s*{[^}]*}\s*from\s*'@arco-design\/web-vue\/es\/icon'\s*\n/gm, '')

  if (src !== before) {
    fs.writeFileSync(file, src)
    console.log('migrated:', file)
  } else {
    console.log('unchanged:', file)
  }
}

for (const t of TARGETS) {
  const stat = fs.statSync(t)
  if (stat.isDirectory()) {
    for (const f of fs.readdirSync(t)) {
      if (f.endsWith('.vue')) migrateFile(path.join(t, f))
    }
  } else {
    migrateFile(t)
  }
}

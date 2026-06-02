#!/usr/bin/env node
/**
 * Arco 表格 → TDesign 表格转换脚本（P2 批次）
 *
 * 将 a-table + #columns + a-table-column 模板结构转换为：
 *   <t-table :columns="xxxColumns" ...>
 *     <template #colKey="{ row }"> 自定义单元格 </template>
 *
 * 并在 script 中生成 columns 数组（保留 i18n 的 t() 调用 → 需文件内已有 useI18n）。
 * 无法静态处理的部分（a-statistic、a-spin 包装等）由后续手工修。
 */
import fs from 'node:fs'

const files = process.argv.slice(2)
for (const file of files) {
let src = fs.readFileSync(file, 'utf8')
const before = src
const generatedColumns = []

// ---- 1. 定位所有 a-table 块并转换（支持 #columns 插槽包裹与多行属性）----
const tableRe = /<a-table((?:\s[^>]*)?)>([\s\S]*?)<\/a-table>/g
let tableIndex = 0
const varNameSeed = 'tableColumns'

src = src.replace(tableRe, (full, attrs, inner) => {
  tableIndex++
  const varName = tableIndex === 1 ? varNameSeed : `${varNameSeed}${tableIndex}`

  // 剥掉 #columns 插槽包裹（如有）
  const colsWrap = inner.match(/<template\s+#columns>([\s\S]*)<\/template>/)
  if (colsWrap) inner = colsWrap[1]

  // 解析原 <a-table> 属性
  const dataRef = attrs.match(/:data="([^"]+)"/)?.[1] || 'tableData'
  const rowKey = (attrs.match(/:?row-key="([^"]+)"/)?.[1] || 'id').replace(/^['"]|['"]$/g, '')
  const hasPagination = !/[:\s]pagination="false"/.test(attrs)
  const loadingAttr = attrs.match(/:loading="([^"]+)"/)?.[1]

  // 列扫描：支持多行属性；列体按配对 </a-table-column> 截取（列不嵌套）
  const columns = []
  let cellTemplates = ''
  const openRe = /<a-table-column\b((?:[^>"]|"[^"]*")*)>/g
  let m
  while ((m = openRe.exec(inner)) !== null) {
    const colAttrs = m[1] || ''
    const colStart = m.index + m[0].length
    const selfClose = /\/\s*$/.test(colAttrs)
    const closeIdx = selfClose ? -1 : inner.indexOf('</a-table-column>', colStart)
    const colBody = selfClose ? '' : closeIdx > -1 ? inner.slice(colStart, closeIdx) : ''
    if (closeIdx > -1) openRe.lastIndex = closeIdx + '</a-table-column>'.length

    const title = colAttrs.match(/:title="([^"]+)"/)?.[1] || "''"
    const colKey = colAttrs.match(/data-index="([^"]+)"/)?.[1]
    const width = colAttrs.match(/:width="(\d+)"/)?.[1]
    const align = colAttrs.match(/align="(\w+)"/)?.[1]
    const ellipsis = /ellipsis/.test(colAttrs)

    // 单元格：取列体内最后一个 #cell 模板（Arco 列只有一个）
    const cellMatch = colBody.match(/<template\s+#cell="[^"]*">([\s\S]*?)<\/template>\s*$/)
    let slotName = null
    let cellHtml = ''
    if (cellMatch) {
      cellHtml = cellMatch[1].replace(/\brecord\b/g, 'row')
      slotName = colKey || cellHtml.match(/row\.(\w+)/)?.[1] || `col_${columns.length}`
    }

    const col = { colKey: colKey || slotName || `col_${columns.length}`, title }
    if (width) col.width = Number(width)
    if (align) col.align = align
    if (ellipsis) col.ellipsis = true
    columns.push(col)

    if (cellMatch && slotName) {
      cellTemplates += `    <template #${slotName}="{ row }">${cellHtml}</template>\n`
    }
  }

  // ---- 生成 t-table 标签 ----
  let tAttrs = `row-key="${rowKey}"`
  tAttrs += ` :data="${dataRef}"`
  tAttrs += ` :columns="${varName}"`
  tAttrs += hasPagination ? ` :pagination="pagination"` : ` :pagination="false"`
  if (loadingAttr) tAttrs += ` :loading="${loadingAttr}"`

  let result = `  <t-table ${tAttrs}>\n`
  if (cellTemplates) result += cellTemplates
  result += `  </t-table>`

  generatedColumns.push({ varName, dataRef, columns })
  return result
})

// ---- 2. 在 script 中插入 columns 定义（首个 import 块之后第一个 const 前）----
if (generatedColumns.length > 0) {
  const hasComputed = /import\s*{[^}]*\bcomputed\b[^}]*}\s*from\s*'vue'/.test(src)
  if (!hasComputed) {
    src = src.replace(/import\s*{\s*ref\s*,/, "import { ref, computed,")
  }
  let insert = '\n'
  for (const g of generatedColumns) {
    const items = g.columns
      .map((c) => {
        const title = c.title.replace(/\$t\(/g, 't(')
        let s = `{ colKey: '${c.colKey}', title: ${title}`
        if (c.width) s += `, width: ${c.width}`
        if (c.align) s += `, align: '${c.align}'`
        if (c.ellipsis) s += `, ellipsis: true`
        return s + ' }'
      })
      .join(',\n    ')
    insert += `/** 表格列定义（由 Arco 列模板迁移，自定义单元格见模板同名插槽） */\n`
    insert += `const ${g.varName} = computed(() => [\n    ${items},\n  ])\n\n`
  }
  // 插到 </script> 前末尾（computed 惰性求值，声明顺序无影响，最安全）
  src = src.replace(/<\/script>/, insert + '</script>')
}

fs.writeFileSync(file, src)
console.log(`tables converted: ${generatedColumns.length}, file: ${file}`)
}

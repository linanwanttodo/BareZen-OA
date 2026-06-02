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

const file = process.argv[2]
let src = fs.readFileSync(file, 'utf8')
const before = src
const generatedColumns = []

// ---- 1. 定位所有 a-table 块并转换 ----
// 逐个处理 <a-table ...> ... </a-table>（不含嵌套 a-table 场景，本仓无）
const tableRe = /<a-table([^>]*)>([\s\S]*?)<\/a-table>/g
let tableIndex = 0
let varNameSeed = 'tableColumns'

src = src.replace(tableRe, (full, attrs, inner) => {
  tableIndex++
  const varName = tableIndex === 1 ? varNameSeed : `${varNameSeed}${tableIndex}`

  // 解析原 <a-table> 属性：data/pagination/loading/row-key
  const dataAttr = attrs.match(/:data="([^"]+)"/)
  const dataRef = dataAttr ? dataAttr[1] : 'tableData'
  const rowKeyRaw = attrs.match(/:?row-key="([^"]+)"/)?.[1] || 'id'
  // :row-key="'id'" 表达式会捕获到 'id'（含引号），剥掉
  const rowKey = rowKeyRaw.replace(/^['"]|['"]$/g, '')
  const hasPagination = !/[:\s]pagination="false"/.test(attrs)
  const loadingAttr = attrs.match(/:loading="([^"]+)"/)?.[1]

  // 解析列
  const colRe = /<a-table-column([^>]*?)(?:\/>|>([\s\S]*?)<\/a-table-column>)/g
  const columns = []
  let cellTemplates = ''
  let m

  while ((m = colRe.exec(inner)) !== null) {
    const colAttrs = m[1] || ''
    const colBody = m[2] || ''
    const title = colAttrs.match(/:title="([^"]+)"/)?.[1] || "''"
    const colKey = colAttrs.match(/data-index="([^"]+)"/)?.[1]
    const width = colAttrs.match(/:width="(\d+)"/)?.[1]
    const align = colAttrs.match(/align="(\w+)"/)?.[1]
    const ellipsis = /ellipsis/.test(colAttrs)

    // 提取自定义单元格：#cell="{ record }" → 模板内 record 改 row
    const cellMatch = colBody.match(/<template\s+#cell="[^"]*">([\s\S]*?)<\/template>/)
    let slotName = null
    let cellHtml = ''
    if (cellMatch) {
      cellHtml = cellMatch[1]
        .replace(/\brecord\b/g, 'row')
        .replace(/<\/?template[^>]*>/g, '')
      // 生成插槽名：优先 data-index，否则用 col_{i}
      slotName = colKey || `col_${columns.length}`
      // record.xxx 引用字段：若无 data-index 但 cell 引用 row.field，从字段推断 colKey
      if (!colKey) {
        const f = cellHtml.match(/row\.(\w+)/)?.[1]
        if (f) slotName = f
      }
    }

    const col = {
      colKey: colKey || slotName || `col_${columns.length}`,
      title,
    }
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
  if (hasPagination) {
    // Arco 分页对象结构 { current, pageSize, total } 与 TDesign pagination 兼容字段：
    // TDesign 用 current/pageSize/total + @page-change
    tAttrs += ` :pagination="pagination"`
  } else {
    tAttrs += ` :pagination="false"`
  }
  if (loadingAttr) tAttrs += ` :loading="${loadingAttr}"`

  let result = `  <t-table ${tAttrs}>\n`
  if (cellTemplates) result += cellTemplates
  result += `  </t-table>`

  // 记录生成的 columns 数组代码
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

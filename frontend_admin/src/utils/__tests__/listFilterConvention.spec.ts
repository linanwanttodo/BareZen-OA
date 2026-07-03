import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { join } from 'path'

/**
 * 列表页筛选区约定（对齐 TDesign Starter 的 common-table）。
 *
 * 外壳改完后，各列表页残留三处让页面显得生硬的写法：
 *  1) 把新增/批量/导出按钮塞进 t-card 的 #title 插槽，与标题挤在同一行；
 *  2) 用 <t-loading> 整块包住表格（项目约定要求改用 t-table 内建 :loading，
 *     否则加载完成时表头与列宽跳变）；
 *  3) 筛选表单用 layout="inline" + 逐字段硬编码宽度，字段宽窄不一、
 *     查询/重置跟在最后一个字段屁股后面，右侧留一大片空白。
 * 这三条都是跨文件的结构性约定，改一页忘一页，单元测试抓不到，用源码扫描兜底。
 *
 * 正确写法见 src/views/system/user/index.vue：
 *   .bz-list-panel 单容器 → t-form(:label-width + t-row/t-col 栅格)
 *   → .bz-list-toolbar 行级操作 → t-table(:loading) → .bz-list-pager
 */
const VIEWS_ROOT = join(process.cwd(), 'src', 'views')

function collectVue(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name)
    return statSync(p).isDirectory() ? collectVue(p) : p.endsWith('.vue') ? [p] : []
  })
}

const rel = (file: string) => file.slice(VIEWS_ROOT.length + 1)

describe('列表页筛选区约定', () => {
  it('t-card 的 #title 插槽内不得放按钮', () => {
    const offenders: string[] = []
    for (const file of collectVue(VIEWS_ROOT)) {
      const src = readFileSync(file, 'utf-8')
      for (const m of src.matchAll(/<template #title>([\s\S]*?)<\/template>/g)) {
        if (/<t-button|<t-space|<t-popconfirm/.test(m[1])) offenders.push(rel(file))
      }
    }
    expect(offenders).toEqual([])
  })

  it('已有 t-table 的页面改用内建 :loading，不得再用 t-loading 整块包裹', () => {
    const offenders: string[] = []
    for (const file of collectVue(VIEWS_ROOT)) {
      const src = readFileSync(file, 'utf-8')
      const tableAt = src.indexOf('<t-table')
      if (tableAt === -1) continue
      const loadingOpen = src.indexOf('<t-loading')
      const loadingClose = src.indexOf('</t-loading>')
      // t-loading 起于表格之前、止于表格之后 —— 即为整块包裹
      if (loadingOpen !== -1 && loadingClose > tableAt) offenders.push(rel(file))
    }
    expect(offenders).toEqual([])
  })

  it('筛选表单用栅格 + label-width，不得是 layout="inline"', () => {
    const offenders: string[] = []
    for (const file of collectVue(VIEWS_ROOT)) {
      const src = readFileSync(file, 'utf-8')
      if (/<t-form[^>]*layout="inline"/.test(src)) offenders.push(rel(file))
    }
    expect(offenders).toEqual([])
  })
})

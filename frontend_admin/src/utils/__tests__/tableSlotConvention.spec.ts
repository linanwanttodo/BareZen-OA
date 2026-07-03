import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { join } from 'path'

/**
 * t-table 单元格插槽名必须是某个 colKey。
 *
 * TDesign 按 colKey 查找同名插槽：插槽名对不上任何列时不报错、不警告，
 * 该列只是退回渲染 row[colKey]——部门/菜单页的 name 列因此整列空白
 * （列叫 deptName/menuName，插槽却叫 #depth），页面看着"只是没数据"，
 * 长期没人当成缺陷。此类跨文件约定漂移单元测试抓不到，用源码扫描兜底。
 */
const VIEWS_ROOT = join(process.cwd(), 'src', 'views')

function collectVue(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name)
    return statSync(p).isDirectory() ? collectVue(p) : p.endsWith('.vue') ? [p] : []
  })
}

/** TDesign 表格保留插槽，不对应 colKey */
const RESERVED = new Set([
  'empty', 'expandedRow', 'dragSort', 'firstColFixed', 'title',
  'footer', 'header', 'filterIcon', 'expandContent',
])

describe('t-table 插槽名约定', () => {
  it('每个 #<colKey>="{ row }" 插槽都对应一个已声明的列', () => {
    const offenders: string[] = []
    for (const file of collectVue(VIEWS_ROOT)) {
      const src = readFileSync(file, 'utf-8')
      const colKeys = new Set([...src.matchAll(/colKey:\s*'([\w-]+)'/g)].map((m) => m[1]))
      if (!colKeys.size) continue
      for (const m of src.matchAll(/<template #([a-zA-Z][\w-]*)="\{\s*row/g)) {
        const slot = m[1]
        if (RESERVED.has(slot) || colKeys.has(slot)) continue
        offenders.push(`${file.slice(VIEWS_ROOT.length + 1)} → #${slot}`)
      }
    }
    expect(offenders).toEqual([])
  })
})

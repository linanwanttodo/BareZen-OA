import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { join } from 'path'

/**
 * 表单约定的源码级守卫。
 *
 * 这两类错误此前全仓静默存在：TDesign 的 t-form 数据 prop 是 `data` 而非
 * Element/Arco 风格的 `model`，写成 :model= 时表单未绑定数据、各 t-form-item
 * 上的 rules 永远不求值（页面照常显示、提交照常成功，所以无人察觉）；
 * validate() 通过时返回 true，写成 `if (errors) return` 会把「校验通过」
 * 当成中断条件。单元测试抓不到这类跨文件约定漂移，故用源码扫描兜底。
 */
function collectVue(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name)
    return statSync(p).isDirectory() ? collectVue(p) : p.endsWith('.vue') ? [p] : []
  })
}

// vitest 以项目根为 cwd；此处不能用 import.meta.url（dev server 下是 http URL）
const VIEWS_ROOT = join(process.cwd(), 'src', 'views')
const files = collectVue(VIEWS_ROOT)
const rel = (f: string) => f.slice(VIEWS_ROOT.length + 1)

describe('t-form 约定', () => {
  it('不使用 :model= 绑定表单数据（正确 prop 是 data）', () => {
    const offenders = files.filter((f) =>
      /<t-form\s[^>]*:model=/.test(readFileSync(f, 'utf-8'))
    )
    expect(offenders).toEqual([])
  })

  it('validate() 闸门一律用 !== true 判定通过', () => {
    const offenders = files
      .map((f) => readFileSync(f, 'utf-8'))
      .filter((src) => /\.validate\(\)/.test(src) && !/!==\s*true/.test(src))
      .map((src) => src.slice(src.indexOf('async function'), src.indexOf('async function') + 60))
    expect(offenders).toEqual([])
  })

  it('t-form-item 用 name= 标识字段（1.20.7 无 field prop，写成 field= 时校验结果挂在空键上，字段级错误显示不出来）', () => {
    const offenders = files
      .filter((f) => /<t-form-item\b[^>]*\bfield=/.test(readFileSync(f, 'utf-8')))
      .map(rel)
    expect(offenders).toEqual([])
  })

  it('写入型表单（非 inline 搜索表单）均挂了 ref 且提交前有闸门', () => {
    const missing: string[] = []
    for (const f of files) {
      const src = readFileSync(f, 'utf-8')
      for (const m of src.matchAll(/<t-form\s((?:(?!<\/t-form>)[\s\S])*?)>/g)) {
        const attrs = m[1]
        if (attrs.includes('layout="inline"') || !/:data=/.test(attrs)) continue
        // type=submit + @submit-success 时 TDesign 提交前自动校验，无需手工闸门
        if (attrs.includes('@submit-success')) continue
        if (!/ref="/.test(attrs)) missing.push(`${rel(f)}: ${/:(\w+)=/.exec(attrs)?.[1] ?? '表单'}`)
      }
    }
    expect(missing).toEqual([])
  })
})

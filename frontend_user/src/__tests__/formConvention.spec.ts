import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { join } from 'path'

/**
 * 表单约定的源码级守卫（用户端）。
 *
 * 管理端曾大面积踩中同一类静默失效：TDesign 的 t-form 数据 prop 是 `data` 而非
 * Element/Arco 风格的 `model`，t-form-item 的字段锚点是 `name` 而非 `field`。
 * 写错时页面照常显示、照常提交，只是 rules 永不求值、字段级错误无处显示，因此
 * 能长期潜伏。两端由同一批 Arco→TDesign 迁移脚本改过，用户端同样中招
 * （11 处 :model=、15 处 field=），修完用本文件钉住，防止再次漂移。
 */
const SRC_ROOT = join(process.cwd(), 'src')

function collectVue(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name)
    return statSync(p).isDirectory() ? collectVue(p) : p.endsWith('.vue') ? [p] : []
  })
}

const files = collectVue(join(SRC_ROOT, 'pages')).concat(collectVue(join(SRC_ROOT, 'components')))
const rel = (f: string) => f.slice(SRC_ROOT.length + 1)

describe('用户端 t-form 约定', () => {
  it('不使用 :model= 绑定表单数据（正确 prop 是 data）', () => {
    expect(files.filter((f) => /<t-form\s[^>]*:model=/.test(readFileSync(f, 'utf-8'))).map(rel))
      .toEqual([])
  })

  it('t-form-item 用 name= 标识字段（1.20.7 无 field prop）', () => {
    expect(files.filter((f) => /<t-form-item\b[^>]*\bfield=/.test(readFileSync(f, 'utf-8'))).map(rel))
      .toEqual([])
  })

  it('validate() 闸门一律用 !== true 判定通过', () => {
    const offenders = files
      .map((f) => readFileSync(f, 'utf-8'))
      .filter((src) => /\.validate\(\)/.test(src) && !/!==\s*true/.test(src))
    expect(offenders).toEqual([])
  })

  it('声明了 rules 的表单要么走 :on-submit，要么挂 ref + validate 闸门', () => {
    // 只约束「有规则要执行」的表单：没声明 rules 的表单挂闸门是空转，
    // 而其必填口径要由业务/DB 约束确定，不靠猜来加规则
    const missing: string[] = []
    for (const file of files) {
      const src = readFileSync(file, 'utf-8')
      for (const m of src.matchAll(/<t-form(?![\w-])/g)) {
        const close = src.indexOf('>', m.index)
        const formEnd = src.indexOf('</t-form>', m.index)
        if (close < 0 || formEnd < 0) continue
        const openTag = src.slice(m.index, close)
        const body = src.slice(close, formEnd)
        if (!/:rules=/.test(body)) continue
        if (openTag.includes('layout="inline"')) continue
        if (openTag.includes('ref=') || openTag.includes(':on-submit')) continue
        missing.push(`${rel(file)}: ${(/:(\w+)=/.exec(openTag)?.[1]) ?? '表单'}`)
      }
    }
    expect(missing).toEqual([])
  })
  it('Form 用 :on-submit 回调，不得写不存在的 @submit-success 事件', () => {
    // tdesign-vue-next 1.20.7 的 Form 只有 onSubmit / onReset 两个 prop 回调，
    // 没有 submit-success 事件。写成 @submit-success 时回调永不触发，
    // 表现为「登录」按钮点了没反应且无任何告警。
    const offenders: string[] = []
    for (const file of files) {
      if (/@submit-success=/.test(readFileSync(file, 'utf-8'))) offenders.push(rel(file))
    }
    expect(offenders).toEqual([])
  })

  it('提交按钮用 type="submit"，不得写不存在的 html-type', () => {
    // TDesign Button 没有 htmlType prop，type 才是原生 submit/reset/button。
    // 写成 html-type 会被当普通 attr 透传，type 仍为默认 "button"，不触发提交。
    const offenders: string[] = []
    for (const file of files) {
      if (/html-type=/.test(readFileSync(file, 'utf-8'))) offenders.push(rel(file))
    }
    expect(offenders).toEqual([])
  })

})

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
        // 搜索表单走 :on-submit：TDesign 提交前自动校验并回调，无需手工闸门。
        // 注意必须写 :on-submit（prop 回调），tdesign-vue-next 1.20.7 的 Form
        // 没有 submit-success 事件，写成 @submit-success 不报错不警告、回调永不触发，
        // 表现为查询按钮点了没反应。
        if (attrs.includes(':on-submit')) continue
        if (!/ref="/.test(attrs)) missing.push(`${rel(f)}: ${/:(\w+)=/.exec(attrs)?.[1] ?? '表单'}`)
      }
    }
    expect(missing).toEqual([])
  })

  it('Form 一律用 :on-submit 回调，不得写不存在的 @submit-success 事件', () => {
    // tdesign-vue-next 1.20.7 的 Form 只提供 onSubmit / onReset 两个 prop 回调
    // （见 node_modules/.../es/form/form.mjs，渲染 form 时绑的是 onSubmit 属性）。
    // 历史上 28 个页面写成 @submit-success，事件名不存在 → Vue 把它当普通
    // 自定义事件监听，validate 通过后调的是 props.onSubmit，回调永不执行，
    // 全站筛选表单的「查询」按钮点了没反应，且不产生任何告警。
    const offenders: string[] = []
    for (const f of files) {
      const src = readFileSync(f, 'utf-8')
      if (/@submit-success=/.test(src)) offenders.push(rel(f))
    }
    expect(offenders).toEqual([])
  })
})

describe('表单提交按钮约定', () => {
  it('提交按钮用 type="submit"，不得写不存在的 html-type', () => {
    // tdesign-vue-next 的 Button 没有 htmlType prop：官方 API 只有
    // theme / variant / size / shape / loading / tag / type，其中 type
    // 就是原生 submit/reset/button。
    // 写成 html-type="submit" 会被当作普通 attr 透传给 <button>，type 仍为
    // 默认 "button"，点击不触发表单提交 —— 表现为「查询」按钮点了没反应，
    // 且 Vue 不会给任何告警（26 个页面中招）。
    const offenders: string[] = []
    for (const f of files) {
      if (/html-type=/.test(readFileSync(f, 'utf-8'))) offenders.push(rel(f))
    }
    expect(offenders).toEqual([])
  })
})

describe('必填标记约定', () => {
  it('t-form-item 不用裸 required，必须写 :rules', () => {
    // tdesign-vue-next 的 t-form-item 没有 required 属性：写了只画一个红星号，
    // 不产生任何校验，空值照样能提交到后端。
    // 必须写 :rules="[{ required: true, message: ... }]"，validate() 才会拦。
    const offenders: string[] = []
    for (const f of files) {
      const src = readFileSync(f, 'utf-8')
      for (const m of src.matchAll(/<t-form-item\b[^>]*>/g)) {
        // 排除已写 :rules 的
        if (/\s:rules=/.test(m[0])) continue
        if (/\srequired>/.test(m[0])) offenders.push(rel(f))
      }
    }
    expect([...new Set(offenders)]).toEqual([])
  })

  it('区间类字段的 name 指向真实字段，不用展示用的 timeRange', () => {
    // name 是校验锚点，必须命中 :data 里的真实键。
    // 起止时间区间历史上写 name="timeRange"（纯展示用），
    // 而控件绑的是 form.startTime / form.endTime —— timeRange 恒为 undefined，
    // 挂 required 要么永不校验，要么在换成 :rules 后永远报错并阻塞提交。
    const offenders: string[] = []
    for (const f of files) {
      // 去掉注释后再匹配，避免「历史说明」注释里的 name="timeRange" 被误判
      const src = readFileSync(f, 'utf-8').replace(/<!--[\s\S]*?-->/g, '')
      if (/name="timeRange"/.test(src)) offenders.push(rel(f))
    }
    expect(offenders).toEqual([])
  })
})

import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { join } from 'path'

/**
 * t-dialog / t-drawer 的显隐绑定约定。
 *
 * tdesign-vue-next 1.20.7 的 Dialog 与 Drawer 只声明了 visible prop，
 * 既没有 modelValue、也只 emit update:visible（见 node_modules 内组件定义）。
 * 因此 <t-dialog v-model="x"> 会绑到一个不存在的 prop 上：
 * 不报错、不警告，x 翻成 true 弹窗也永远不出现。
 *
 * 全站曾因此一次性静默失效 59 处弹窗——所有「新增 / 编辑 / 详情」入口点了没反应，
 * 表现为「只能查看和删除，不能编辑和添加」。这类跨文件的结构性写法单元测试抓不到，用源码扫描兜底。
 *
 * 正确写法：<t-dialog v-model:visible="x" />
 */
const VIEWS_ROOT = join(process.cwd(), 'src')

function collectVue(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name)
    return statSync(p).isDirectory() ? collectVue(p) : p.endsWith('.vue') ? [p] : []
  })
}

const rel = (file: string) => file.slice(VIEWS_ROOT.length + 1)

describe('t-dialog / t-drawer 显隐绑定约定', () => {
  it('t-dialog / t-drawer 不得用裸 v-model 绑显隐，必须是 v-model:visible', () => {
    const offenders: string[] = []
    for (const file of collectVue(VIEWS_ROOT)) {
      const src = readFileSync(file, 'utf-8')
      src.split('\n').forEach((line, i) => {
        // 只看开标签所在行；v-model:visible 不含 v-model=" 字面量，不会误报
        if (!/^\s*<t-(dialog|drawer)\b/.test(line)) return
        if (/\sv-model="/.test(line)) offenders.push(`${rel(file)}:${i + 1}`)
      })
    }
    expect(offenders).toEqual([])
  })

  it('裸 v-model 确实绑不上 Dialog（对照实验，锁住库行为）', () => {
    // 若将来升级到支持 modelValue 的版本，本用例会失败并提示上一条守卫可放宽
    const dialogSource = readFileSync(
      join(process.cwd(), 'node_modules/tdesign-vue-next/es/dialog/dialog.mjs'), 'utf-8')
    expect(dialogSource).not.toMatch(/modelValue\s*:/)
    expect(dialogSource).toMatch(/update:visible/)
  })
})

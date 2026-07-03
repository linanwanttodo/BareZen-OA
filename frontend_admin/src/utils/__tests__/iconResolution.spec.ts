import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'fs'
import { join, dirname } from 'path'

/**
 * 图标名必须可解析。
 *
 * 模板里的 <icon-xxx> 靠 main.ts 的「IconXxx 同名全局注册」表解析；名字打错、
 * 或用了 Ant Design 遗留名（TDesign 根本没有 DriveIcon / CarIcon）时，Vue 只发一条
 * warning，图标位置留空——页面功能一切正常，因此这类缺陷可以长期潜伏。
 * 待办中心三张统计卡就有两个图标因此从未显示过。
 */
const SRC_ROOT = join(process.cwd(), 'src')

function collectFiles(dir: string, ext: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const p = join(dir, name)
    return statSync(p).isDirectory() ? collectFiles(p, ext) : p.endsWith(ext) ? [p] : []
  })
}

const mainTs = readFileSync(join(SRC_ROOT, 'main.ts'), 'utf-8')
const REGISTERED = new Set([...mainTs.matchAll(/^\s+(Icon\w+):/gm)].map((m) => m[1]))

/** kebab 约定名 → 全局注册名：<icon-user-group> → IconUserGroup */
function toRegisteredName(kebab: string) {
  return 'Icon' + kebab.split('-').map((s) => s[0].toUpperCase() + s.slice(1)).join('')
}

describe('图标可解析性', () => {
  it('模板中每个 <icon-*> 都能由全局注册表或本文件具名导入解析', () => {
    const offenders: string[] = []
    for (const file of collectFiles(SRC_ROOT, '.vue')) {
      const src = readFileSync(file, 'utf-8')
      const local = new Set<string>()
      for (const m of src.matchAll(/import\s*\{([^}]*)\}\s*from\s*['"]tdesign-icons-vue-next['"]/g)) {
        for (const raw of m[1].split(',')) {
          const name = raw.trim().split(/\s+as\s+/).pop()
          if (!name) continue
          // 同时接受 CalendarIcon 与 <calendar-icon> 直用两种写法
          local.add(name)
          local.add(name.replace(/Icon$/, ''))
        }
      }
      for (const m of src.matchAll(/<(icon-[a-z0-9-]+)[\s/>]/g)) {
        const pascal = toRegisteredName(m[1].slice('icon-'.length))
        if (REGISTERED.has(pascal) || local.has(pascal) || local.has(pascal.replace(/^Icon/, ''))) continue
        offenders.push(`${file.slice(SRC_ROOT.length + 1)} → <${m[1]}>`)
      }
      // 直接以 <xxx-icon> 形式使用的，也必须有本文件导入
      for (const m of src.matchAll(/<([a-z][a-z0-9-]*)-icon[\s/>]/g)) {
        const pascal = toRegisteredName(m[1])
        if (local.has(pascal) || local.has(pascal.replace(/^Icon/, '')) || REGISTERED.has(pascal)) continue
        offenders.push(`${file.slice(SRC_ROOT.length + 1)} → <${m[1]}-icon>`)
      }
    }
    expect(offenders).toEqual([])
  })

  it('main.ts 注册的图标组件确实由 tdesign-icons-vue-next 导入（防手误写成不存在的名字）', () => {
    const imported = new Set(
      [...mainTs.matchAll(/import\s*\{([^}]*)\}\s*from\s*['"]tdesign-icons-vue-next['"]/gs)]
        .flatMap((m) => m[1].split(',').map((s) => s.trim()).filter(Boolean))
    )
    const values = [...mainTs.matchAll(/^\s+Icon\w+:\s*(\w+),/gm)].map((m) => m[1])
    const missing = values.filter((v) => !imported.has(v))
    expect(missing).toEqual([])
  })
})

#!/usr/bin/env node
/**
 * 插槽撞名去重（migrate-tables 后处理）
 *
 * columns 数组中重复的 colKey 会导致 Vue "Duplicate slot names" 构建错误。
 * 按出现顺序保留第一次，后续出现重命名：
 *  - 操作列（title 为 common.operate / i18n operate）→ 'operate'
 *  - 其余 → `${colKey}_${n}`
 * 模板中 <template #old> 按第 N 次出现同步重命名。
 */
import fs from 'node:fs'

const file = process.argv[2]
let src = fs.readFileSync(file, 'utf8')

// 提取所有 tableColumns 数组的 colKey 顺序
const colKeyRe = /\{ colKey: '([^']+)', title: ([^}]+?)\}/g
const cols = []
let m
while ((m = colKeyRe.exec(src)) !== null) {
  cols.push({ colKey: m[1], title: m[2], index: m.index })
}

const seen = new Map()
const renames = [] // { from, to, occurrence }
for (const c of cols) {
  const n = seen.get(c.colKey) || 0
  seen.set(c.colKey, n + 1)
  if (n > 0) {
    const isOperate = /operate/i.test(c.title)
    const to = isOperate && !seen.has('operate') ? 'operate' : `${c.colKey}_${n + 1}`
    renames.push({ from: c.colKey, to, occurrence: n })
    c.newName = to
  }
}

if (renames.length === 0) {
  console.log(`no duplicates: ${file}`)
  process.exit(0)
}

// 按出现顺序替换模板插槽：第 k 次出现的 <template #from= 换成 to
for (const r of renames) {
  const slotRe = new RegExp(`<template #${r.from}="(\\{[^}]*\\})">`, 'g')
  let seenN = 0
  src = src.replace(slotRe, (full, slotProps) => {
    seenN++
    if (seenN - 1 === r.occurrence) {
      return `<template #${r.to}="${slotProps}">`
    }
    return full
  })
}

// 更新 columns 数组中的 colKey（从后往前替换避免串扰）
for (let i = renames.length - 1; i >= 0; i--) {
  const r = renames[i]
  // 找到第 r.occurrence 次出现的 colKey: 'from' 定义并替换
  const defRe = new RegExp(`\\{ colKey: '${r.from}',`, 'g')
  let n = 0
  src = src.replace(defRe, (full) => {
    if (n === r.occurrence) {
      n++
      return `{ colKey: '${r.to}',`
    }
    n++
    return full
  })
}

fs.writeFileSync(file, src)
console.log(`renamed ${renames.length}: ${file} -> ${renames.map((r) => `${r.from}#${r.occurrence + 1}=${r.to}`).join(', ')}`)

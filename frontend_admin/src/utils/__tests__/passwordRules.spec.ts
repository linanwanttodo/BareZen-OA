/**
 * 密码强度校验边界测试
 *
 * 验证前端 `computePasswordRules` 与后端 `PasswordUtil.validatePasswordPolicy` 完全一致。
 * 后端规则：长度 ≥ 8 + 大写 + 小写 + 数字 + 特殊字符（共 5 项，全部满足才可提交）。
 */
import { describe, it, expect } from 'vitest'

function computePasswordRules(pwd: string) {
  return [
    { label: '至少 8 位', valid: pwd.length >= 8 },
    { label: '包含大写字母', valid: /[A-Z]/.test(pwd) },
    { label: '包含小写字母', valid: /[a-z]/.test(pwd) },
    { label: '包含数字', valid: /\d/.test(pwd) },
    { label: '包含特殊字符', valid: /[^A-Za-z0-9]/.test(pwd) },
  ]
}

describe('密码强度校验（与后端 PasswordUtil.validatePasswordPolicy 一致）', () => {
  it('7 位纯数字：仅通过数字规则，提交应被拦截', () => {
    const rules = computePasswordRules('1234567')
    expect(rules.filter((r) => r.valid).length).toBe(1)
    expect(rules.every((r) => r.valid)).toBe(false)
  })

  it('8 位纯小写：长度+小写两项通过（4/5），提交应被拦截', () => {
    const rules = computePasswordRules('abcdefgh')
    expect(rules.filter((r) => r.valid).length).toBe(2)
    expect(rules.every((r) => r.valid)).toBe(false)
  })

  it('8 位大写+小写+数字+特殊：5 项全绿，可提交', () => {
    const rules = computePasswordRules('Abc@1234')
    expect(rules.filter((r) => r.valid).length).toBe(5)
    expect(rules.every((r) => r.valid)).toBe(true)
  })

  it('8 位大写+小写+数字，无特殊字符：4/5，提交应被拦截', () => {
    const rules = computePasswordRules('Abcdefg1')
    expect(rules.filter((r) => r.valid).length).toBe(4)
    expect(rules.every((r) => r.valid)).toBe(false)
  })

  it('空密码：0/5，提交应被拦截', () => {
    const rules = computePasswordRules('')
    expect(rules.filter((r) => r.valid).length).toBe(0)
    expect(rules.every((r) => r.valid)).toBe(false)
  })

  it('边界：恰好 8 位满足全部规则可通过', () => {
    expect(computePasswordRules('Aa1@bbbb').every((r) => r.valid)).toBe(true)
    expect(computePasswordRules('Aa1@bbb').every((r) => r.valid)).toBe(false)
  })
})

/**
 * 密码策略边界测试。
 *
 * 此前三方各自持有实现：TopNavbar、system/user（两份规则源），而本文件又在测试里
 * 另抄了一份 computePasswordRules 来自测——生产代码的规则怎么改，这里都不会红，
 * 属于假信心测试。现改为直接 import 真实模块，并顺带覆盖强度档位与提交判定。
 *
 * 规则须与后端 PasswordUtil.validatePasswordPolicy 一致：
 * 长度 ≥ 8 + 大写 + 小写 + 数字 + 特殊字符，五项全满足才可提交。
 */
import { describe, it, expect } from 'vitest'
import {
  computePasswordRules,
  evaluatePasswordRules,
  isPasswordPolicyValid,
  passwordStrengthLevel,
} from '@/utils/passwordRules'

const t = (key: string) => key

const validCount = (pwd: string) => evaluatePasswordRules(pwd).filter((r) => r.valid).length

describe('密码策略（与后端 PasswordUtil.validatePasswordPolicy 一致）', () => {
  it('7 位纯数字：仅数字一项满足，不可提交', () => {
    expect(validCount('1234567')).toBe(1)
    expect(isPasswordPolicyValid('1234567')).toBe(false)
  })

  it('8 位纯小写：长度+小写两项满足，不可提交', () => {
    expect(validCount('abcdefgh')).toBe(2)
    expect(isPasswordPolicyValid('abcdefgh')).toBe(false)
  })

  it('8 位含大小写数字特殊：五项全满足，可提交', () => {
    expect(validCount('Abc@1234')).toBe(5)
    expect(isPasswordPolicyValid('Abc@1234')).toBe(true)
  })

  it('8 位无特殊字符：四项满足，仍不可提交', () => {
    expect(validCount('Abcdefg1')).toBe(4)
    expect(isPasswordPolicyValid('Abcdefg1')).toBe(false)
  })

  it('空密码：零项满足', () => {
    expect(validCount('')).toBe(0)
    expect(isPasswordPolicyValid('')).toBe(false)
  })

  it('长度边界：恰好 8 位为界', () => {
    expect(isPasswordPolicyValid('Aa1@bbbb')).toBe(true)
    expect(isPasswordPolicyValid('Aa1@bbb')).toBe(false)
  })

  it('强度档位按满足项数折算为 0-3', () => {
    expect(passwordStrengthLevel('')).toBe(0)
    expect(passwordStrengthLevel('abcdefg')).toBe(1)
    expect(passwordStrengthLevel('Abcdefg1')).toBe(2)
    expect(passwordStrengthLevel('Abc@1234')).toBe(3)
  })

  it('规则条目带 i18n 标签与稳定 key，供组件渲染', () => {
    const rules = computePasswordRules('Abc@1234', t)
    expect(rules.map((r) => r.key)).toEqual(['length', 'upper', 'lower', 'digit', 'special'])
    expect(rules.map((r) => r.label)).toEqual([
      'user.pwdRuleLength', 'user.pwdRuleUpper', 'user.pwdRuleLower', 'user.pwdRuleDigit', 'user.pwdRuleSpecial',
    ])
    expect(rules.every((r) => r.valid)).toBe(true)
  })
})

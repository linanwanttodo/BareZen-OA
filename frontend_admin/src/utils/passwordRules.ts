/**
 * 密码策略的前端单一实现。
 *
 * 此前 TopNavbar 与 system/user 各有一份 computePasswordRules（user 页还另有一份
 * batchPasswordRules），且 passwordRules.spec.ts 是在测试文件里又抄了一份来断言——
 * 三处实现任何一处改动都不会让测试变红。现统一到本模块，测试直接 import 本模块。
 *
 * 规则须与后端 PasswordUtil.validatePasswordPolicy 保持一致：
 * 长度 ≥ 8 + 大写 + 小写 + 数字 + 特殊字符，五项全满足才可提交。
 */

export interface PasswordRule {
  label: string
  valid: boolean
  key: 'length' | 'upper' | 'lower' | 'digit' | 'special'
}

export function evaluatePasswordRules(pwd: string): Array<{ key: PasswordRule['key']; valid: boolean }> {
  return [
    { key: 'length', valid: pwd.length >= 8 },
    { key: 'upper', valid: /[A-Z]/.test(pwd) },
    { key: 'lower', valid: /[a-z]/.test(pwd) },
    { key: 'digit', valid: /\d/.test(pwd) },
    { key: 'special', valid: /[^A-Za-z0-9]/.test(pwd) },
  ]
}

/** i18n 文案在此注入，保持纯函数便于测试 */
export function computePasswordRules(pwd: string, t: (key: string) => string): PasswordRule[] {
  const labels: Record<PasswordRule['key'], string> = {
    length: t('user.pwdRuleLength'),
    upper: t('user.pwdRuleUpper'),
    lower: t('user.pwdRuleLower'),
    digit: t('user.pwdRuleDigit'),
    special: t('user.pwdRuleSpecial'),
  }
  return evaluatePasswordRules(pwd).map((r) => ({ ...r, label: labels[r.key] }))
}

/** 0-3 四档强度：满足项数折算，5 项全满足为最强 */
export function passwordStrengthLevel(pwd: string): number {
  const satisfied = evaluatePasswordRules(pwd).filter((r) => r.valid).length
  if (satisfied >= 5) return 3
  if (satisfied >= 3) return 2
  if (satisfied >= 1) return 1
  return 0
}

/** 与后端策略一致的「可否提交」判定 */
export function isPasswordPolicyValid(pwd: string): boolean {
  return evaluatePasswordRules(pwd).every((r) => r.valid)
}

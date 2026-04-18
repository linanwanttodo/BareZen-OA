/**
 * 轻量级 className 合并工具（shadcn 风格 cn 的零依赖实现）
 * 支持字符串、条件对象与 falsy 值过滤，等价于 clsx 的核心能力。
 */
export type ClassValue =
  | string
  | number
  | null
  | undefined
  | false
  | Record<string, boolean | null | undefined>

export function cn(...inputs: ClassValue[]): string {
  const out: string[] = []
  for (const input of inputs) {
    if (!input) continue
    if (typeof input === 'string' || typeof input === 'number') {
      out.push(String(input))
    } else if (typeof input === 'object') {
      for (const key in input) {
        if (input[key]) out.push(key)
      }
    }
  }
  return out.join(' ')
}

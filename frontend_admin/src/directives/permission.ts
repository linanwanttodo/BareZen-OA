import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-permission 权限控制指令
 *
 * 使用方式：
 *  <div v-permission="'system:user:add'"> visible only with permission </div>
 *  <div v-permission="['system:user:add', 'system:user:edit']"> needs ALL permissions </div>
 *
 * 逻辑：
 * - admin 角色（*:*:*）始终可见
 * - 字符串参数：检查单个权限
 * - 数组参数：检查所有权限（AND）
 */
export const vPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const userStore = useUserStore()
    const permissions = userStore.permissions || []

    // admin 通配符放行
    if (permissions.includes('*:*:*')) {
      return
    }

    const requiredPerms = binding.value
    let hasPermission = false

    if (typeof requiredPerms === 'string') {
      hasPermission = permissions.includes(requiredPerms)
    } else if (Array.isArray(requiredPerms)) {
      hasPermission = requiredPerms.every((perm: string) => permissions.includes(perm))
    } else {
      hasPermission = true
    }

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}

/**
 * v-permission-any 权限控制指令（任一权限即可）
 *
 * 使用方式：
 *  <div v-permission-any="['system:user:add', 'system:user:edit']">
 */
export const vPermissionAny: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const userStore = useUserStore()
    const permissions = userStore.permissions || []

    if (permissions.includes('*:*:*')) {
      return
    }

    const requiredPerms = binding.value
    let hasPermission = false

    if (Array.isArray(requiredPerms)) {
      hasPermission = requiredPerms.some((perm: string) => permissions.includes(perm))
    } else if (typeof requiredPerms === 'string') {
      hasPermission = permissions.includes(requiredPerms)
    } else {
      hasPermission = true
    }

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}

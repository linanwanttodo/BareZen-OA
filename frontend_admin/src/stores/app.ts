import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface TagItem {
  path: string
  name: string
  title: string
  affix?: boolean
}

export const useAppStore = defineStore('app', () => {
  const tagsList = ref<TagItem[]>([
    { path: '/dashboard', name: 'Dashboard', title: '仪表盘', affix: true },
  ])
  const activeTag = ref('/dashboard')

  /** 添加标签 */
  function addTag(tag: TagItem) {
    const exists = tagsList.value.some((t) => t.path === tag.path)
    if (!exists) {
      tagsList.value.push(tag)
    }
    activeTag.value = tag.path
  }

  /** 关闭标签 */
  function closeTag(path: string) {
    const index = tagsList.value.findIndex((t) => t.path === path)
    if (index === -1) return null
    const tag = tagsList.value[index]
    if (tag.affix) return null

    tagsList.value.splice(index, 1)

    // 如果关闭的是当前激活的标签，切换到相邻标签
    if (activeTag.value === path) {
      const nextTag = tagsList.value[index] || tagsList.value[index - 1]
      return nextTag?.path || '/dashboard'
    }
    return null
  }

  /** 关闭其他标签 */
  function closeOtherTags(path: string) {
    tagsList.value = tagsList.value.filter((t) => t.affix || t.path === path)
    activeTag.value = path
  }

  /** 关闭所有标签（保留 affix） */
  function closeAllTags() {
    tagsList.value = tagsList.value.filter((t) => t.affix)
    const last = tagsList.value[tagsList.value.length - 1]
    return last?.path || '/dashboard'
  }

  /** 关闭指定标签左侧的全部标签（affix 固定标签保留） */
  function closeLeftTags(path: string) {
    const idx = tagsList.value.findIndex((t) => t.path === path)
    if (idx === -1) return
    tagsList.value = tagsList.value.filter((t, i) => t.affix || i >= idx)
  }

  /** 关闭指定标签右侧的全部标签（affix 固定标签保留） */
  function closeRightTags(path: string) {
    const idx = tagsList.value.findIndex((t) => t.path === path)
    if (idx === -1) return
    tagsList.value = tagsList.value.filter((t, i) => t.affix || i <= idx)
  }

  /**
   * 拖拽排序：把 from 位置的标签移动到 to 位置。
   * affix 固定标签钉在首位，不参与移动（避免被拖到仪表盘之后）。
   */
  function moveTag(from: number, to: number) {
    if (from === to) return
    const list = tagsList.value
    // affix 恒在数组头部，可移动区间是 [fixedCount, list.length - 1]
    const fixedCount = list.filter((t) => t.affix).length
    if (from < fixedCount || to < fixedCount) return
    if (from < 0 || to < 0 || from >= list.length || to >= list.length) return
    const [item] = list.splice(from, 1)
    list.splice(to, 0, item)
  }

  /** 重新加载当前标签页：先销毁 keep-alive 缓存再跳回自身 */
  const refreshKey = ref(0)
  function reloadTag(path: string) {
    if (activeTag.value !== path) activeTag.value = path
    refreshKey.value += 1
  }

  return {
    tagsList,
    activeTag,
    refreshKey,
    addTag,
    closeTag,
    closeOtherTags,
    closeAllTags,
    closeLeftTags,
    closeRightTags,
    moveTag,
    reloadTag,
  }
})

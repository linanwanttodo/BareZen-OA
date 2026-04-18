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

  return {
    tagsList,
    activeTag,
    addTag,
    closeTag,
    closeOtherTags,
    closeAllTags,
  }
})

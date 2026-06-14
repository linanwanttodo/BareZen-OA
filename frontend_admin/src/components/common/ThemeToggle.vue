<template>
  <t-tooltip :content="isDark ? '切换亮色模式' : '切换暗色模式'">
    <t-button variant="text" shape="square" size="small" @click="onToggle">
      <template #icon>
        <sunny-icon v-if="isDark" />
        <moon-icon v-else />
      </template>
    </t-button>
  </t-tooltip>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { MoonIcon, SunnyIcon } from 'tdesign-icons-vue-next'
import { isDarkMode, toggleTheme } from '@/utils/theme'

const isDark = ref(false)

function onToggle() {
  isDark.value = toggleTheme()
}

onMounted(() => {
  isDark.value = isDarkMode()
})
</script>

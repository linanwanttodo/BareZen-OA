<template>
  <span :class="containerCls">
    <span class="trend-icon-container">
      <svg
        v-if="type === 'down'"
        width="16"
        height="16"
        viewBox="0 0 16 16"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path d="M11.5 8L8 11.5L4.5 8" stroke="currentColor" stroke-width="1.5" />
        <path d="M8 11L8 4" stroke="currentColor" stroke-width="1.5" />
      </svg>
      <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <path d="M4.5 8L8 4.5L11.5 8" stroke="currentColor" stroke-width="1.5" />
        <path d="M8 5V12" stroke="currentColor" stroke-width="1.5" />
      </svg>
    </span>
    <span>{{ describe }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const { type, isReverseColor } = defineProps({
  type: { type: String, default: '' },
  describe: { type: [String, Number], default: undefined },
  // 反色卡片（品牌蓝底白字）内使用，改为中性反白配色
  isReverseColor: { type: Boolean, default: false },
})

/**
 * 涨跌配色沿用官方语义：up 用 error 红、down 用 success 绿（中国区惯例，涨=红、跌=绿），
 * 与 TDesign Starter dashboard/top-panel 的 Trend 组件保持一致。
 */
const containerCls = computed(() => [
  'trend-container',
  {
    'trend-container__reverse': isReverseColor,
    'trend-container__up': !isReverseColor && type === 'up',
    'trend-container__down': !isReverseColor && type === 'down',
  },
])
</script>

<style scoped>
.trend-container__up,
.trend-container__down,
.trend-container__reverse {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.trend-container__up {
  color: var(--td-error-color);
}
.trend-container__up .trend-icon-container {
  background: var(--td-error-color-2);
}

.trend-container__down {
  color: var(--td-success-color);
}
.trend-container__down .trend-icon-container {
  background: var(--td-success-color-2);
}

.trend-container__reverse {
  color: var(--td-text-color-anti);
}
.trend-container__reverse .trend-icon-container {
  background: var(--td-brand-color-5);
}

.trend-icon-container {
  margin-right: var(--td-comp-margin-s);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
</style>

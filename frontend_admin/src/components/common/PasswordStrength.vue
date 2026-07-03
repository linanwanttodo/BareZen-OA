<template>
  <div class="pwd-strength">
    <div v-if="showBars && value" class="mb-2">
      <div class="flex gap-1">
        <div
          v-for="i in 4"
          :key="i"
          class="h-1.5 flex-1 rounded"
          :class="barClass(i)"
        ></div>
      </div>
      <p class="mt-1 text-xs" :class="levelTextClass">{{ levelText }}</p>
    </div>

    <div v-if="value" class="rounded-md bg-[var(--td-bg-color-secondarycontainer)] p-3 text-xs">
      <p class="mb-1 font-medium">{{ $t('user.pwdPolicyTitle') }}</p>
      <p
        v-for="rule in rules"
        :key="rule.key"
        class="flex items-center gap-1.5"
        :class="rule.valid ? 'text-[var(--td-success-color)]' : 'text-[var(--td-text-color-placeholder)]'"
      >
        <icon-check v-if="rule.valid" size="12px" />
        <icon-minus v-else size="12px" />
        <span>{{ rule.label }}</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { computePasswordRules, passwordStrengthLevel } from '@/utils/passwordRules'

const props = withDefaults(defineProps<{ value: string; showBars?: boolean }>(), { showBars: false })

const { t } = useI18n()
const rules = computed(() => computePasswordRules(props.value || '', t))
const level = computed(() => passwordStrengthLevel(props.value || ''))

// 档位着色：文字色与格子色各自显式声明，避免用字符串替换推导 class 名
const LEVEL_TEXT_CLASS = [
  'text-[var(--td-error-color)]',
  'text-[var(--td-warning-color)]',
  'text-[var(--td-warning-color)]',
  'text-[var(--td-success-color)]',
]
const LEVEL_BAR_CLASS = [
  'bg-[var(--td-error-color)]',
  'bg-[var(--td-warning-color)]',
  'bg-[var(--td-warning-color)]',
  'bg-[var(--td-success-color)]',
]

const levelText = computed(() => t(`user.pwdStrength${level.value}`))
const levelTextClass = computed(() => LEVEL_TEXT_CLASS[level.value])

/** 前 level 格按档位着色，其余保持中性描边 */
function barClass(barIndex: number): string {
  return barIndex <= level.value ? LEVEL_BAR_CLASS[level.value] : 'bg-[var(--td-component-stroke)]'
}
</script>

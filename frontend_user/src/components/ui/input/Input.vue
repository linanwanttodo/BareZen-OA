<script setup lang="ts">
defineProps<{ modelValue?: string | number }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <input
    :value="modelValue"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    v-bind="$attrs"
    class="flex h-9 w-full rounded border border-[var(--border)] bg-[var(--wecom-bg-card)] px-3 py-1 text-sm text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:border-[var(--primary)] disabled:cursor-not-allowed disabled:opacity-50"
  />
</template>

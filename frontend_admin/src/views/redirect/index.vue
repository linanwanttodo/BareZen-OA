<template>
  <div class="redirect-container">
    <p>{{ $t('redirect.redirecting') }}</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  const { params, query } = route
  // params.path 可能是字符串或字符串数组，统一拍平为数组再拼接
  const raw = params.path
  const path = Array.isArray(raw) ? raw.join('/') : (raw ?? '')
  router.replace({ path: `/${path}`, query })
})
</script>

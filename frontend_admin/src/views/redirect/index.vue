<template>
  <div class="redirect-container">
    <p>正在跳转...</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  const { params, query } = route
  const path = (params as any).path as string
  router.replace({ path: `/${Array.isArray(path) ? path.join('/') : path}`, query })
})
</script>

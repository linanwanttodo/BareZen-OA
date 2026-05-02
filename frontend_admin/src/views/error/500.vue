<template>
  <ErrorPage code="500" :message="$t('error.500')" />
</template>

<script setup lang="ts">
import ErrorPage from './index.vue'
</script>

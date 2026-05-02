<template>
  <ErrorPage code="404" :message="$t('error.404')" />
</template>

<script setup lang="ts">
import ErrorPage from './index.vue'
</script>

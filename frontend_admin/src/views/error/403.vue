<template>
  <ErrorPage code="403" :message="$t('error.403')" />
</template>

<script setup lang="ts">
import ErrorPage from './index.vue'
</script>

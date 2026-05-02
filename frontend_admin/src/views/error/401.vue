<template>
  <ErrorPage code="401" :message="$t('error.401')" />
</template>

<script setup lang="ts">
import ErrorPage from './index.vue'
</script>

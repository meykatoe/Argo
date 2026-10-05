<script setup lang="ts">
import { watchEffect } from 'vue'
import { useI18n } from 'vue-i18n'
import SiteLayout from '@/layouts/SiteLayout.vue'

const { t } = useI18n()

// 標題跟著語言變
watchEffect(() => {
  document.title = t('site.title')
})
</script>

<template>
  <SiteLayout />
</template>

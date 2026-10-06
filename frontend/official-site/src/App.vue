<script setup lang="ts">
import { onMounted, watchEffect } from 'vue'
import { useI18n } from 'vue-i18n'
import { ping } from '@/api/auth'
import SiteLayout from '@/layouts/SiteLayout.vue'
import { blocked } from '@/utils/access'
import BlockedView from '@/views/BlockedView.vue'

const { t } = useI18n()

// 標題跟著語言變
watchEffect(() => {
  document.title = t('site.title')
})

// 啟動就問後端一次，被封鎖的 IP 不論打開哪一頁都會看到封鎖畫面，其他錯誤不影響
onMounted(() => ping().catch(() => undefined))
</script>

<template>
  <BlockedView v-if="blocked" />
  <SiteLayout v-else />
</template>

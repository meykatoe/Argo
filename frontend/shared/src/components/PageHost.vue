<script setup lang="ts">
import { computed, watchEffect, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuNode } from '../types'
import { firstPath, pageNodes } from '../utils/menu'

const props = defineProps<{ menu: MenuNode[]; token: string; pages: Record<string, Component> }>()
const emit = defineEmits<{ unauthorized: [] }>()

const route = useRoute()
const router = useRouter()

const allowed = computed(() => pageNodes(props.menu).map((n) => n.path))

// 不在可見選單內的網址，導向第一個可用頁面
watchEffect(() => {
  const first = firstPath(props.menu)
  if (first && !allowed.value.includes(route.path)) {
    router.replace(first)
  }
})

const page = computed(() => (allowed.value.includes(route.path) ? props.pages[route.path] : undefined))
</script>

<template>
  <p v-if="allowed.length === 0" class="note">目前尚無可用功能</p>
  <component :is="page" v-else-if="page" :key="route.path" :token="token" @unauthorized="emit('unauthorized')" />
  <p v-else-if="allowed.includes(route.path)" class="note">此功能尚未開放</p>
</template>

<style scoped>
.note {
  color: var(--color-muted);
}
</style>

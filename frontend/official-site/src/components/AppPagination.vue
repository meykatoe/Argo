<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{ page: number; totalPages: number }>()
const emit = defineEmits<{ change: [page: number] }>()

// 目前頁前後各兩頁
const pages = computed(() => {
  const start = Math.max(1, props.page - 2)
  const end = Math.min(props.totalPages, props.page + 2)
  const list: number[] = []
  for (let i = start; i <= end; i++) list.push(i)
  return list
})
</script>

<template>
  <nav v-if="totalPages > 1" class="pager" :aria-label="t('pager.label')">
    <button :disabled="page <= 1" @click="emit('change', page - 1)">{{ t('pager.prev') }}</button>
    <button v-if="pages[0]! > 1" @click="emit('change', 1)">1</button>
    <span v-if="pages[0]! > 2">…</span>
    <button
      v-for="p in pages"
      :key="p"
      :class="{ active: p === page }"
      @click="emit('change', p)"
    >
      {{ p }}
    </button>
    <span v-if="pages[pages.length - 1]! < totalPages - 1">…</span>
    <button v-if="pages[pages.length - 1]! < totalPages" @click="emit('change', totalPages)">
      {{ totalPages }}
    </button>
    <button :disabled="page >= totalPages" @click="emit('change', page + 1)">{{ t('pager.next') }}</button>
  </nav>
</template>

<style scoped>
.pager {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 24px;
}

button {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

button.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>

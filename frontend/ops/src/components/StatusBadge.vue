<script setup lang="ts">
import type { MonitorLevel } from '@/types'
import { LEVEL_LABEL } from '@/utils/monitor'

defineProps<{ level: MonitorLevel }>()
</script>

<template>
  <span class="badge" :data-level="level">
    <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
      <template v-if="level === 'NORMAL'">
        <circle cx="8" cy="8" r="7" fill="#0ca30c" />
        <path d="M4.6 8.2l2.4 2.4 4.4-4.8" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
      </template>
      <template v-else-if="level === 'WARNING'">
        <path d="M8 1.5l7 12.5H1z" fill="#fab219" stroke="#fab219" stroke-width="1.5" stroke-linejoin="round" />
        <path d="M8 6v3.6" stroke="#0b0b0b" stroke-width="1.6" stroke-linecap="round" />
        <circle cx="8" cy="11.6" r="0.9" fill="#0b0b0b" />
      </template>
      <template v-else>
        <circle cx="8" cy="8" r="7" fill="#d03b3b" />
        <path d="M5.4 5.4l5.2 5.2M10.6 5.4l-5.2 5.2" stroke="#fff" stroke-width="1.8" stroke-linecap="round" />
      </template>
    </svg>
    <span class="label">{{ LEVEL_LABEL[level] }}</span>
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text);
}
</style>

<script setup lang="ts">
import { ref } from 'vue'
import type { AuditLog } from '@/types'
import { actionLabel, formatTime, roleLabel, summarize } from '@/utils/audit'

defineProps<{ log: AuditLog }>()

const open = ref(false)
</script>

<template>
  <tr :class="{ failed: !log.success }">
    <td class="time">{{ formatTime(log.createdAt) }}</td>
    <td>
      {{ log.username }}
      <span class="hint">{{ roleLabel(log.role) }}</span>
    </td>
    <td>
      {{ actionLabel(log.action) }}
      <span v-if="!log.success" class="bad">失敗</span>
    </td>
    <td>{{ summarize(log) }}</td>
    <td class="hint">{{ log.ip ?? '-' }}</td>
    <td>
      <button v-if="log.detail || log.targetId" type="button" class="link" @click="open = !open">
        {{ open ? '收合' : '詳情' }}
      </button>
    </td>
  </tr>
  <tr v-if="open" class="detail">
    <td colspan="6">
      <div>
        對象：{{ log.targetType ?? '-' }} {{ log.targetId ?? '' }} · 編號 {{ log.id }} ·
        {{ log.userAgent ?? '-' }}
      </div>
      <pre>{{ JSON.stringify(log.detail, null, 2) }}</pre>
    </td>
  </tr>
</template>

<style scoped>
td {
  padding: 8px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: top;
}

.time {
  white-space: nowrap;
}

.hint {
  font-size: 12px;
  color: var(--color-muted);
}

.bad {
  margin-left: 4px;
  padding: 0 6px;
  border-radius: 4px;
  background: #fee4e2;
  color: #d92d20;
  font-size: 12px;
}

.failed {
  background: #fff8f7;
}

.link {
  padding: 2px 8px;
}

.detail td {
  background: #f6f6f3;
  font-size: 13px;
}

pre {
  margin: 6px 0 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

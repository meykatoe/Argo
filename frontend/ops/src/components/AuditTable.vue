<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, searchAuditLogs } from '@/api/ops'
import type { AuditAction, AuditLog, PageResult } from '@/types'
import { ACTION_LABELS, toIso } from '@/utils/audit'
import { REFRESH_MS, useAutoRefresh } from '@/utils/autoRefresh'
import { errorText } from '@/utils/error'
import AuditRow from './AuditRow.vue'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const username = ref('')
const action = ref<AuditAction | ''>('')
const result = ref<'' | 'ok' | 'fail'>('')
const from = ref('')
const to = ref('')
const page = ref(1)
const data = ref<PageResult<AuditLog> | null>(null)
const loading = ref(false)
const error = ref('')

const updatedAt = ref<Date | null>(null)

// auto 為真代表背景自動更新：不閃載入中，後端也不逐次記錄查看
async function load(auto = false) {
  if (!auto) loading.value = true
  error.value = ''
  try {
    data.value = await searchAuditLogs(props.token, {
      username: username.value.trim(),
      action: action.value,
      success: result.value === '' ? undefined : result.value === 'ok',
      from: toIso(from.value),
      to: toIso(to.value),
      page: page.value,
      size: 50,
      refresh: auto || undefined,
    })
    updatedAt.value = new Date()
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('unauthorized')
    }
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function go(p: number) {
  page.value = p
  load()
}

// 只在第一頁自動更新，翻到後面幾頁時列表會一直位移，反而看不清楚
const { enabled } = useAutoRefresh(
  () => load(true),
  () => page.value === 1 && !loading.value,
  REFRESH_MS,
)

const clock = (d: Date) => d.toLocaleTimeString('zh-TW', { hour12: false })

onMounted(() => load())
</script>

<template>
  <section>
    <form class="bar" @submit.prevent="search">
      <input v-model="username" placeholder="帳號" aria-label="帳號" />
      <select v-model="action" aria-label="動作">
        <option value="">全部動作</option>
        <option v-for="(label, key) in ACTION_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
      <select v-model="result" aria-label="結果">
        <option value="">全部結果</option>
        <option value="ok">成功</option>
        <option value="fail">失敗</option>
      </select>
      <label>從 <input v-model="from" type="datetime-local" /></label>
      <label>到 <input v-model="to" type="datetime-local" /></label>
      <button type="submit" class="primary" :disabled="loading">查詢</button>
    </form>

    <div class="live">
      <label>
        <input v-model="enabled" type="checkbox" />
        自動更新（每 {{ REFRESH_MS / 1000 }} 秒）
      </label>
      <span v-if="enabled && page > 1" class="hint">翻到其他頁時暫停自動更新</span>
      <span v-if="updatedAt" class="hint">上次更新 {{ clock(updatedAt) }}</span>
      <button type="button" :disabled="loading" @click="load()">重新整理</button>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-else-if="loading" class="hint">載入中</p>

    <table v-if="data">
      <thead>
        <tr>
          <th>時間</th>
          <th>人員</th>
          <th>動作</th>
          <th>內容</th>
          <th>IP</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <AuditRow v-for="l in data.items" :key="l.id" :log="l" />
      </tbody>
    </table>
    <p v-if="data && data.items.length === 0" class="hint">沒有符合的紀錄</p>

    <div v-if="data && data.totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1 || loading" @click="go(page - 1)">上一頁</button>
      <span>{{ data.page }} / {{ data.totalPages }}（共 {{ data.total }} 筆）</span>
      <button type="button" :disabled="page >= data.totalPages || loading" @click="go(page + 1)">
        下一頁
      </button>
    </div>
  </section>
</template>

<style scoped>
.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

select {
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font: inherit;
  background: #fff;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

th {
  padding: 8px;
  text-align: left;
  border-bottom: 2px solid var(--color-border);
  font-size: 13px;
  color: var(--color-muted);
}

.live {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 14px;
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: #d92d20;
}
</style>

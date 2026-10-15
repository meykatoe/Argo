<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, listRefunds, markRefunded } from '@/api/ops'
import type { PageResult, RefundRow, RefundStatus } from '@/types'
import { REFRESH_MS, useAutoRefresh } from '@/utils/autoRefresh'
import { formatTime } from '@/utils/audit'
import { errorText } from '@/utils/error'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const SIZE = 20
const status = ref<RefundStatus>('REQUIRED')
const page = ref(1)
const data = ref<PageResult<RefundRow> | null>(null)
const loading = ref(false)
const busyId = ref<number | null>(null)
const error = ref('')

function failed(e: unknown): string {
  if (e instanceof ApiError && e.status === 401) emit('unauthorized')
  return errorText(e)
}

// auto 為真代表背景更新，不閃載入中
async function load(auto = false) {
  if (!auto) loading.value = true
  try {
    data.value = await listRefunds(props.token, status.value, page.value, SIZE)
  } catch (e) {
    error.value = failed(e)
  } finally {
    loading.value = false
  }
}

function pick(s: RefundStatus) {
  status.value = s
  page.value = 1
  error.value = ''
  load()
}

function go(p: number) {
  page.value = p
  load()
}

async function confirmRefunded(row: RefundRow) {
  if (busyId.value !== null) return
  const msg = `確定已在綠界後台退款 ${row.currency} ${row.amount}（訂單 ${row.orderNo ?? '-'}）嗎？此動作會記入稽核紀錄。`
  if (!window.confirm(msg)) return
  busyId.value = row.id
  error.value = ''
  try {
    await markRefunded(props.token, row.id)
  } catch (e) {
    error.value = failed(e)
  } finally {
    busyId.value = null
    await load()
  }
}

const { enabled } = useAutoRefresh(
  () => load(true),
  () => page.value === 1 && busyId.value === null && !loading.value,
)

onMounted(() => load())
</script>

<template>
  <section>
    <p class="hint">
      客人在訂單取消或過期後才付款成功的款項會列在這裡。請到綠界後台手動退款，再按「標記已退款」。
    </p>
    <div class="bar">
      <button type="button" :class="{ on: status === 'REQUIRED' }" :aria-pressed="status === 'REQUIRED'" @click="pick('REQUIRED')">
        待退款
      </button>
      <button type="button" :class="{ on: status === 'REFUNDED' }" :aria-pressed="status === 'REFUNDED'" @click="pick('REFUNDED')">
        已退款
      </button>
      <label class="auto">
        <input v-model="enabled" type="checkbox" />
        每 {{ REFRESH_MS / 1000 }} 秒自動更新
      </label>
    </div>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading && !data">載入中…</p>

    <table v-if="data" aria-label="退款清單">
      <thead>
        <tr>
          <th>訂單</th>
          <th>訂單狀態</th>
          <th>交易編號</th>
          <th>金額</th>
          <th>{{ status === 'REQUIRED' ? '收款時間' : '退款時間' }}</th>
          <th v-if="status === 'REQUIRED'"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in data.items" :key="r.id">
          <td>{{ r.orderNo ?? '-' }}</td>
          <td>{{ r.orderStatus ?? '-' }}</td>
          <td class="mono">{{ r.tradeNo }}</td>
          <td>{{ r.currency }} {{ r.amount }}</td>
          <td>{{ formatTime(status === 'REQUIRED' ? r.paidAt : (r.refundedAt ?? r.paidAt)) }}</td>
          <td v-if="status === 'REQUIRED'">
            <button type="button" :disabled="busyId !== null" @click="confirmRefunded(r)">標記已退款</button>
          </td>
        </tr>
        <tr v-if="data.items.length === 0">
          <td :colspan="status === 'REQUIRED' ? 6 : 5" class="hint">目前沒有資料</td>
        </tr>
      </tbody>
    </table>

    <div v-if="data && data.totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1 || loading" @click="go(page - 1)">上一頁</button>
      <span>{{ data.page }} / {{ data.totalPages }}（共 {{ data.total }} 筆）</span>
      <button type="button" :disabled="page >= data.totalPages || loading" @click="go(page + 1)">下一頁</button>
    </div>
  </section>
</template>

<style scoped>
.hint {
  color: var(--color-muted);
  font-size: 13px;
}

.error {
  color: #d03b3b;
}

.bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 12px 0;
}

.bar button.on {
  font-weight: 700;
  border-color: currentColor;
}

.auto {
  margin-left: auto;
  font-size: 13px;
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

td {
  padding: 8px;
  border-bottom: 1px solid var(--color-border);
}

.mono {
  font-family: ui-monospace, monospace;
}

.pager {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
}
</style>

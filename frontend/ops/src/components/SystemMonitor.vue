<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiError, getMonitor } from '@/api/ops'
import type { Monitor } from '@/types'
import { REFRESH_MS, useAutoRefresh } from '@/utils/autoRefresh'
import { errorText } from '@/utils/error'
import {
  alertText,
  appColor,
  appLabel,
  clockTime,
  formatMs,
  formatUptime,
  worstLevel,
} from '@/utils/monitor'
import Sparkline from './Sparkline.vue'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const RANGES = [5, 15, 60]
const minutes = ref(5)
const data = ref<Monitor | null>(null)
const loading = ref(false)
const error = ref('')
const updatedAt = ref<Date | null>(null)

// 背景更新不閃
async function load(auto = false) {
  if (!auto) loading.value = true
  error.value = ''
  try {
    data.value = await getMonitor(props.token, minutes.value)
    updatedAt.value = new Date()
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) emit('unauthorized')
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

const { enabled } = useAutoRefresh(() => load(true), () => !loading.value, REFRESH_MS)

const summary = computed(() => {
  const d = data.value
  if (!d) return ''
  if (d.alerts.length === 0) return '目前沒有預警，系統負載正常'
  return `有 ${d.alerts.length} 項預警，請優先處理標示「危險」的項目`
})

const times = computed(() => data.value?.history.map((p) => p.at) ?? [])

const tiles = computed(() => {
  const d = data.value
  if (!d) return []
  const s = d.system
  const h = d.history
  const level = (codes: string[]) => worstLevel(d.alerts, codes)
  return [
    {
      key: 'cpu',
      label: 'CPU 使用率',
      value: `${s.cpuPercent}%`,
      detail: `${s.threads} 條執行緒`,
      values: h.map((p) => p.cpu),
      max: 100,
      unit: '%',
      level: level(['CPU_HIGH']),
    },
    {
      key: 'heap',
      label: '記憶體（堆積）',
      value: `${s.heapPercent}%`,
      detail: `${s.heapUsedMb} / ${s.heapMaxMb} MB`,
      values: h.map((p) => p.heap),
      max: 100,
      unit: '%',
      level: level(['HEAP_HIGH']),
    },
    {
      key: 'pool',
      label: '資料庫連線池',
      value: `${s.poolPercent}%`,
      detail: `使用 ${s.poolActive} / ${s.poolMax}，等待 ${s.poolPending}`,
      values: h.map((p) => p.pool),
      max: 100,
      unit: '%',
      level: level(['DB_POOL_HIGH', 'DB_POOL_WAITING']),
    },
    {
      key: 'gc',
      label: '垃圾回收佔比',
      value: `${s.gcPercent}%`,
      detail: `已運行 ${formatUptime(s.uptimeSeconds)}`,
      values: h.map((p) => p.gc),
      max: undefined,
      unit: '%',
      level: 'NORMAL' as const,
    },
  ]
})

const maxRequests = computed(() => Math.max(1, ...(data.value?.apps.map((a) => a.requests) ?? [])))
const hasUnknown = computed(() => data.value?.apps.some((a) => a.app === 'unknown') ?? false)
const num = (n: number) => n.toLocaleString('zh-TW')
const recent = computed(() => (data.value?.history ?? []).slice(-60).reverse())

onMounted(() => load())
</script>

<template>
  <section class="monitor">
    <form class="bar" @submit.prevent="load()">
      <label>
        統計範圍
        <select v-model.number="minutes" aria-label="統計範圍" @change="load()">
          <option v-for="m in RANGES" :key="m" :value="m">最近 {{ m }} 分鐘</option>
        </select>
      </label>
      <label>
        <input v-model="enabled" type="checkbox" />
        自動更新（每 {{ REFRESH_MS / 1000 }} 秒）
      </label>
      <span v-if="updatedAt" class="hint">上次更新 {{ clockTime(updatedAt.getTime()) }}</span>
      <button type="submit" :disabled="loading">重新整理</button>
    </form>

    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <div v-if="data" :class="{ stale: loading }">
      <div class="banner" :data-level="data.level">
        <StatusBadge :level="data.level" />
        <span class="summary">{{ summary }}</span>
        <ul v-if="data.alerts.length" class="alerts">
          <li v-for="a in data.alerts" :key="a.code + (a.app ?? '')">
            <StatusBadge :level="a.level" />
            <span>{{ alertText(a) }}</span>
          </li>
        </ul>
      </div>
      <p class="hint">
        資源類預警要連續超標約 1 分鐘才會出現；前端類預警看最近 5 分鐘，且至少 20 筆請求才判斷。
      </p>

      <h2>系統資源</h2>
      <div class="tiles">
        <article v-for="t in tiles" :key="t.key" class="tile">
          <header>
            <span class="name">{{ t.label }}</span>
            <StatusBadge v-if="t.level !== 'NORMAL'" :level="t.level" />
          </header>
          <div class="value">{{ t.value }}</div>
          <div class="detail">{{ t.detail }}</div>
          <Sparkline :label="t.label" :values="t.values" :times="times" :unit="t.unit" :max="t.max" />
        </article>
      </div>

      <h2>各前端的請求</h2>
      <p class="hint">統計最近 {{ data.windowMinutes }} 分鐘。錯誤率只算伺服器錯誤（5xx）。</p>
      <table v-if="data.apps.length" class="apps">
        <thead>
          <tr>
            <th>前端</th>
            <th class="num">請求量</th>
            <th class="num">每分鐘</th>
            <th class="num">錯誤率</th>
            <th class="num">平均</th>
            <th class="num">p95</th>
            <th class="num">p99</th>
            <th class="num">最慢</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in data.apps" :key="a.app">
            <td>
              <span class="key" :style="{ background: appColor(a.app) }" aria-hidden="true"></span>
              {{ appLabel(a.app) }}
            </td>
            <td class="num">
              <span class="count">{{ num(a.requests) }}</span>
              <span
                class="bar-fill"
                :style="{ width: `${(a.requests / maxRequests) * 80}px`, background: appColor(a.app) }"
                aria-hidden="true"
              ></span>
            </td>
            <td class="num">{{ a.perMinute }}</td>
            <td class="num">{{ a.errorRate }}%（{{ num(a.errors5xx) }}）</td>
            <td class="num">{{ formatMs(a.avgMs) }}</td>
            <td class="num">{{ formatMs(a.p95Ms) }}</td>
            <td class="num">{{ formatMs(a.p99Ms) }}</td>
            <td class="num">{{ formatMs(a.maxMs) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="hint">這段時間沒有請求紀錄</p>
      <p v-if="hasUnknown" class="hint">
        「未標示來源」是沒有帶前端標頭的請求，例如指令列工具、爬蟲或舊版前端。
      </p>

      <div class="ranks">
        <div>
          <h2>最慢的端點（p95）</h2>
          <table v-if="data.slowest.length">
            <thead>
              <tr>
                <th>端點</th>
                <th class="num">請求量</th>
                <th class="num">p95</th>
                <th class="num">最慢</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="e in data.slowest" :key="e.app + e.method + e.path">
                <td>
                  <span class="key" :style="{ background: appColor(e.app) }" aria-hidden="true"></span>
                  <span class="mono">{{ e.method }} {{ e.path }}</span>
                  <span class="hint"> {{ appLabel(e.app) }}</span>
                </td>
                <td class="num">{{ num(e.requests) }}</td>
                <td class="num">{{ formatMs(e.p95Ms) }}</td>
                <td class="num">{{ formatMs(e.maxMs) }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="hint">這段時間沒有資料</p>
        </div>
        <div>
          <h2>最常出錯的端點</h2>
          <table v-if="data.failing.length">
            <thead>
              <tr>
                <th>端點</th>
                <th class="num">請求量</th>
                <th class="num">5xx 次數</th>
                <th class="num">錯誤率</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="e in data.failing" :key="e.app + e.method + e.path">
                <td>
                  <span class="key" :style="{ background: appColor(e.app) }" aria-hidden="true"></span>
                  <span class="mono">{{ e.method }} {{ e.path }}</span>
                  <span class="hint"> {{ appLabel(e.app) }}</span>
                </td>
                <td class="num">{{ num(e.requests) }}</td>
                <td class="num">{{ num(e.errors5xx) }}</td>
                <td class="num">{{ e.errorRate }}%</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="hint">這段時間沒有伺服器錯誤</p>
        </div>
      </div>

      <details class="raw">
        <summary>查看資源取樣數據（最近 {{ recent.length }} 筆）</summary>
        <table>
          <thead>
            <tr>
              <th>時間</th>
              <th class="num">CPU</th>
              <th class="num">記憶體</th>
              <th class="num">連線池</th>
              <th class="num">GC</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in recent" :key="p.at">
              <td>{{ clockTime(p.at) }}</td>
              <td class="num">{{ p.cpu }}%</td>
              <td class="num">{{ p.heap }}%</td>
              <td class="num">{{ p.pool }}%</td>
              <td class="num">{{ p.gc }}%</td>
            </tr>
          </tbody>
        </table>
      </details>
    </div>
    <p v-else-if="loading" class="hint">載入中…</p>
  </section>
</template>

<style scoped>
h2 {
  margin: 24px 0 4px;
  font-size: 16px;
}

.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.bar label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-muted);
}

select {
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font: inherit;
  background: #fff;
}

.stale {
  opacity: 0.6;
}

.banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-top: 16px;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
}

.summary {
  color: var(--color-text);
}

.alerts {
  flex-basis: 100%;
  margin: 4px 0 0;
  padding: 8px 0 0;
  border-top: 1px solid var(--color-border);
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
}

.alerts li {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tiles {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 12px;
}

.tile {
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
}

.tile header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 22px;
}

.name {
  color: var(--color-muted);
  font-size: 14px;
}

.value {
  margin-top: 4px;
  font-size: 28px;
  font-weight: 600;
}

.detail {
  margin-bottom: 8px;
  color: var(--color-muted);
  font-size: 13px;
}

table {
  width: 100%;
  margin-top: 8px;
  border-collapse: collapse;
  background: #fff;
  font-size: 14px;
}

th,
td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

th {
  color: var(--color-muted);
  font-weight: 500;
  white-space: nowrap;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}

.key {
  display: inline-block;
  width: 12px;
  height: 3px;
  margin-right: 8px;
  vertical-align: middle;
  border-radius: 2px;
}

.count {
  margin-right: 8px;
}

.bar-fill {
  display: inline-block;
  height: 8px;
  vertical-align: middle;
  border-radius: 0 4px 4px 0;
}

.ranks {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 0 24px;
}

.raw {
  margin-top: 24px;
}

.raw summary {
  cursor: pointer;
  color: var(--color-muted);
  font-size: 14px;
}

.hint {
  color: var(--color-muted);
  font-size: 13px;
}

.error {
  color: var(--color-primary);
}
</style>

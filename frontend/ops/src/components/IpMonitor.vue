<script setup lang="ts">
import { computed, inject, onMounted, ref, type ComputedRef } from 'vue'
import { ApiError, blockIp, listIpBlocks, listIps, unblockIp } from '@/api/ops'
import type { IpActivity, IpBlock, PageResult } from '@/types'
import { formatTime } from '@/utils/audit'
import { errorText } from '@/utils/error'
import { MENU_CODES } from '@/utils/menu'
import IpBlockDialog from './IpBlockDialog.vue'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const codes = inject<ComputedRef<Set<string>>>(MENU_CODES, computed(() => new Set<string>()))
const canBlock = computed(() => codes.value.has('security.ips.block'))

const days = ref(7)
const keyword = ref('')
const page = ref(1)
const data = ref<PageResult<IpActivity> | null>(null)
const blocks = ref<IpBlock[]>([])
const loading = ref(false)
const error = ref('')

// 目前開著的封鎖對話框
const dialog = ref<{ ip: string; lock: boolean } | null>(null)
const dialogBusy = ref(false)
const dialogError = ref('')

function failed(e: unknown): string {
  if (e instanceof ApiError && e.status === 401) emit('unauthorized')
  return errorText(e)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [activity, active] = await Promise.all([
      listIps(props.token, { days: days.value, keyword: keyword.value.trim() || undefined, page: page.value, size: 20 }),
      listIpBlocks(props.token),
    ])
    data.value = activity
    blocks.value = active
  } catch (e) {
    error.value = failed(e)
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

async function submitBlock(ip: string, reason: string, hours: number | null) {
  dialogBusy.value = true
  dialogError.value = ''
  try {
    await blockIp(props.token, ip, reason, hours)
    dialog.value = null
    await load()
  } catch (e) {
    dialogError.value = failed(e)
  } finally {
    dialogBusy.value = false
  }
}

async function unblock(ip: string) {
  if (!window.confirm(`確定要解除封鎖 ${ip} 嗎？解除後這個 IP 可以立即恢復使用。`)) return
  error.value = ''
  try {
    await unblockIp(props.token, ip)
    await load()
  } catch (e) {
    error.value = failed(e)
  }
}

function openBlock(ip: string, lock: boolean) {
  dialogError.value = ''
  dialog.value = { ip, lock }
}

const expiry = (iso: string | null) => (iso ? `到 ${formatTime(iso)}` : '永久')

onMounted(load)
</script>

<template>
  <section>
    <form class="bar" @submit.prevent="search">
      <label>
        統計範圍
        <select v-model.number="days" aria-label="統計範圍" @change="search">
          <option :value="1">今天</option>
          <option :value="3">近 3 天</option>
          <option :value="7">近 7 天</option>
          <option :value="30">近 30 天</option>
        </select>
      </label>
      <input v-model="keyword" placeholder="IP 開頭，例如 203.0.113" aria-label="IP 搜尋" />
      <button type="submit" class="primary" :disabled="loading">搜尋</button>
      <button v-if="canBlock" type="button" class="spaced" @click="openBlock('', false)">封鎖指定 IP</button>
    </form>

    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <h2>異常 IP</h2>
    <p class="hint">依問題次數由多到少排序。限速：請求太頻繁被擋；登入失敗：登入或註冊被拒；封鎖後嘗試：被封鎖後仍繼續連線。</p>
    <table v-if="data">
      <thead>
        <tr>
          <th>IP</th>
          <th>被限速</th>
          <th>登入失敗</th>
          <th>封鎖後嘗試</th>
          <th>最後出現</th>
          <th>狀態</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in data.items" :key="r.ip" :class="{ blocked: r.blocked === 1 }">
          <td class="mono">{{ r.ip }}</td>
          <td class="num">{{ r.rateLimited }}</td>
          <td class="num">{{ r.loginFailed }}</td>
          <td class="num">{{ r.blockedHits }}</td>
          <td class="time">{{ formatTime(r.lastSeen) }}</td>
          <td>
            <span v-if="r.blocked === 1" class="tag">已封鎖（{{ expiry(r.blockExpiresAt) }}）</span>
            <span v-else class="hint">未封鎖</span>
          </td>
          <td>
            <template v-if="canBlock">
              <button v-if="r.blocked === 1" type="button" class="small" @click="unblock(r.ip)">解除封鎖</button>
              <button v-else type="button" class="small danger-text" @click="openBlock(r.ip, true)">封鎖</button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="data && data.items.length === 0" class="hint">這段時間沒有異常紀錄</p>

    <div v-if="data && data.totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1 || loading" @click="go(page - 1)">上一頁</button>
      <span>{{ data.page }} / {{ data.totalPages }}（共 {{ data.total }} 個 IP）</span>
      <button type="button" :disabled="page >= data.totalPages || loading" @click="go(page + 1)">下一頁</button>
    </div>

    <h2>目前封鎖中的 IP</h2>
    <table v-if="blocks.length > 0" class="blocks">
      <thead>
        <tr>
          <th>IP</th>
          <th>原因</th>
          <th>封鎖者</th>
          <th>封鎖時間</th>
          <th>期限</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="b in blocks" :key="b.ip">
          <td class="mono">{{ b.ip }}</td>
          <td>{{ b.reason }}</td>
          <td>{{ b.blockedBy }}</td>
          <td class="time">{{ formatTime(b.createdAt) }}</td>
          <td>{{ expiry(b.expiresAt) }}</td>
          <td>
            <button v-if="canBlock" type="button" class="small" @click="unblock(b.ip)">解除封鎖</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else-if="data" class="hint">目前沒有封鎖中的 IP</p>

    <IpBlockDialog
      v-if="dialog"
      :ip="dialog.ip"
      :lock-ip="dialog.lock"
      :busy="dialogBusy"
      :error="dialogError"
      @submit="submitBlock"
      @close="dialog = null"
    />
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
}

.bar label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-muted);
  font-size: 14px;
}

.spaced {
  margin-left: auto;
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

td {
  padding: 8px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: top;
}

tr.blocked {
  background: #f6f6f3;
}

.mono {
  font-family: ui-monospace, monospace;
  word-break: break-all;
}

.num {
  text-align: right;
}

.time {
  white-space: nowrap;
}

.tag {
  padding: 1px 8px;
  border-radius: 10px;
  background: #fee4e2;
  color: #d92d20;
  font-size: 12px;
}

.small {
  padding: 2px 10px;
  font-size: 12px;
}

.danger-text {
  color: #d92d20;
  border-color: #d92d20;
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.hint {
  margin: 4px 0 8px;
  color: var(--color-muted);
  font-size: 13px;
}

.error {
  color: #d92d20;
}
</style>

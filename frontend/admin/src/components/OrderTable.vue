<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, searchOrders } from '@/api/admin'
import type { AdminOrderRow, OrderStatus, PageResult } from '@/types'
import { formatPrice } from '@/utils/discount'
import { errorText } from '@/utils/error'
import { formatTime, STATUS_OPTIONS, STATUS_TEXT } from '@/utils/order'
import OrderDetail from './OrderDetail.vue'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const keyword = ref('')
const status = ref<OrderStatus | ''>('')
const page = ref(1)
const data = ref<PageResult<AdminOrderRow> | null>(null)
const loading = ref(false)
const error = ref('')
const opened = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await searchOrders(props.token, {
      keyword: keyword.value.trim(),
      status: status.value || undefined,
      page: page.value,
      size: 20,
    })
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

onMounted(load)
</script>

<template>
  <section>
    <form class="bar" @submit.prevent="search">
      <select v-model="status" aria-label="狀態" @change="search">
        <option value="">全部狀態</option>
        <option v-for="s in STATUS_OPTIONS" :key="s" :value="s">{{ STATUS_TEXT[s] }}</option>
      </select>
      <input v-model="keyword" placeholder="訂單編號、Email 或姓名" />
      <button type="submit" class="primary">搜尋</button>
    </form>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="loading" class="hint">載入中</p>

    <table v-if="data">
      <thead>
        <tr>
          <th>訂單編號</th>
          <th>狀態</th>
          <th>顧客</th>
          <th>件數</th>
          <th>合計</th>
          <th>下單時間</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="o in data.items" :key="o.orderNo">
          <td>{{ o.orderNo }}</td>
          <td>{{ STATUS_TEXT[o.status] }}</td>
          <td>{{ o.customerName }}<br /><small>{{ o.customerEmail }}</small></td>
          <td>{{ o.itemCount }}</td>
          <td>{{ formatPrice(o.total) }}</td>
          <td>{{ formatTime(o.createdAt) }}</td>
          <td><button type="button" @click="opened = o.orderNo">查看</button></td>
        </tr>
      </tbody>
    </table>
    <p v-if="data && data.items.length === 0" class="hint">沒有符合的訂單</p>

    <div v-if="data && data.totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1 || loading" @click="go(page - 1)">上一頁</button>
      <span>{{ data.page }} / {{ data.totalPages }}</span>
      <button type="button" :disabled="page >= data.totalPages || loading" @click="go(page + 1)">
        下一頁
      </button>
    </div>

    <OrderDetail
      v-if="opened"
      :token="token"
      :order-no="opened"
      @close="opened = null"
      @unauthorized="emit('unauthorized')"
    />
  </section>
</template>

<style scoped>
select {
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font: inherit;
  background: #fff;
}

.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

th,
td {
  padding: 8px;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

th {
  border-bottom-width: 2px;
  font-size: 13px;
  color: var(--color-muted);
}

small {
  color: var(--color-muted);
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

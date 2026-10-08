<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, ref, type ComputedRef } from 'vue'
import { ApiError, cancelOrder, completeOrder, getOrder, setOrderNote, shipOrder } from '@/api/admin'
import type { AdminOrder } from '@/types'
import { errorText } from '@/utils/error'
import { formatPrice } from '@/utils/discount'
import { MENU_CODES } from '@shared/utils/menu'
import { cancelText, formatTime, STATUS_TEXT } from '@/utils/order'

const props = defineProps<{ token: string; orderNo: string }>()
const emit = defineEmits<{ close: []; changed: []; unauthorized: [] }>()

const codes = inject<ComputedRef<Set<string>>>(MENU_CODES, computed(() => new Set<string>()))
const canManage = computed(() => codes.value.has('order.manage'))

const order = ref<AdminOrder | null>(null)
const error = ref('')
const box = ref<HTMLElement | null>(null)
const tracking = ref('')
const note = ref('')
const busy = ref(false)

// 操作成功後更新內容並通知列表
function apply(o: AdminOrder) {
  order.value = o
  note.value = o.staffNote ?? ''
  emit('changed')
}

async function run(fn: () => Promise<AdminOrder>) {
  busy.value = true
  error.value = ''
  try {
    apply(await fn())
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('unauthorized')
    }
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}

function ship() {
  run(() => shipOrder(props.token, props.orderNo, tracking.value.trim()))
}

function complete() {
  if (window.confirm('確定將這筆訂單標記為已完成？')) {
    run(() => completeOrder(props.token, props.orderNo))
  }
}

function cancel() {
  if (window.confirm('確定取消這筆訂單？庫存會退回，此操作無法復原。')) {
    run(() => cancelOrder(props.token, props.orderNo))
  }
}

function saveNote() {
  run(() => setOrderNote(props.token, props.orderNo, note.value))
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    emit('close')
  }
}

onMounted(async () => {
  window.addEventListener('keydown', onKey)
  box.value?.focus()
  try {
    order.value = await getOrder(props.token, props.orderNo)
    note.value = order.value.staffNote ?? ''
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('unauthorized')
    }
    error.value = errorText(e)
  }
})
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div class="backdrop" @click.self="emit('close')">
      <div ref="box" class="box" role="dialog" aria-modal="true" :aria-label="`訂單 ${orderNo}`" tabindex="-1">
        <header>
          <h2>訂單 {{ orderNo }}</h2>
          <button type="button" aria-label="關閉" @click="emit('close')">關閉</button>
        </header>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="!order && !error" class="hint">載入中</p>
        <template v-if="order">
          <dl>
            <dt>狀態</dt>
            <dd>
              {{ STATUS_TEXT[order.status] }}
              <span v-if="order.cancelReason">（{{ cancelText(order.cancelReason) }}）</span>
            </dd>
            <dt>下單時間</dt>
            <dd>{{ formatTime(order.createdAt) }}</dd>
            <dt>付款時間</dt>
            <dd>{{ formatTime(order.paidAt) }}</dd>
            <dt v-if="order.shippedAt">出貨時間</dt>
            <dd v-if="order.shippedAt">
              {{ formatTime(order.shippedAt) }}
              <span v-if="order.trackingNo">（單號 {{ order.trackingNo }}）</span>
            </dd>
            <dt v-if="order.completedAt">完成時間</dt>
            <dd v-if="order.completedAt">{{ formatTime(order.completedAt) }}</dd>
            <dt v-if="order.cancelledAt">取消時間</dt>
            <dd v-if="order.cancelledAt">{{ formatTime(order.cancelledAt) }}</dd>
            <dt>顧客</dt>
            <dd>
              {{ order.customerName }}｜{{ order.customerEmail }}｜{{ order.customerPhone }}
              <span v-if="order.customerId === null">（訪客）</span>
            </dd>
            <dt>收件</dt>
            <dd>
              {{ order.recipientName }}｜{{ order.recipientPhone }}<br />
              {{ order.postalCode }} {{ order.city }}{{ order.address }}
            </dd>
            <dt>付款</dt>
            <dd v-if="order.payment">
              {{ order.payment.status === 'SUCCEEDED' ? '成功' : '失敗' }}
              <span v-if="order.payment.cardLast4">（卡號末四碼 {{ order.payment.cardLast4 }}）</span>
              <span v-if="order.payment.failureCode">（{{ order.payment.failureCode }}）</span>
            </dd>
            <dd v-else>尚無付款紀錄</dd>
          </dl>

          <table>
            <thead>
              <tr>
                <th>卡號</th>
                <th>卡名</th>
                <th>單價</th>
                <th>數量</th>
                <th>小計</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in order.items" :key="i.cardId">
                <td>{{ i.cardSetId }}</td>
                <td>{{ i.cardName }}<br /><small>{{ i.cardNameEn }}</small></td>
                <td>{{ formatPrice(i.unitPrice) }}</td>
                <td>{{ i.quantity }}</td>
                <td>{{ formatPrice(i.subtotal) }}</td>
              </tr>
            </tbody>
          </table>
          <p class="sum">
            商品 {{ formatPrice(order.subtotal) }}　運費 {{ formatPrice(order.shippingFee) }}　
            <strong>合計 {{ formatPrice(order.total) }}</strong>
          </p>

          <section v-if="canManage" class="actions">
            <form v-if="order.status === 'PAID'" @submit.prevent="ship">
              <input v-model="tracking" maxlength="50" placeholder="物流單號（可留空）" aria-label="物流單號" />
              <button type="submit" class="primary" :disabled="busy">出貨</button>
            </form>
            <button v-if="order.status === 'SHIPPED'" type="button" class="primary" :disabled="busy" @click="complete">
              標記完成
            </button>
            <button
              v-if="order.status === 'PENDING_PAYMENT' || order.status === 'PAID'"
              type="button"
              class="danger"
              :disabled="busy"
              @click="cancel"
            >
              取消訂單
            </button>
            <form class="note" @submit.prevent="saveNote">
              <textarea v-model="note" maxlength="500" rows="2" placeholder="內部備註（顧客看不到）" aria-label="內部備註" />
              <button type="submit" :disabled="busy">儲存備註</button>
            </form>
          </section>
          <p v-else-if="order.staffNote" class="sum">備註：{{ order.staffNote }}</p>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(0 0 0 / 55%);
}

.box {
  width: min(720px, 94vw);
  max-height: 90vh;
  overflow: auto;
  padding: 20px 24px;
  border-radius: 8px;
  background: #fff;
  outline: none;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h2 {
  margin: 0;
  font-size: 18px;
}

dl {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 6px 12px;
}

dt {
  color: var(--color-muted);
}

dd {
  margin: 0;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 6px 8px;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

small {
  color: var(--color-muted);
}

.sum {
  text-align: right;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.actions form {
  display: flex;
  gap: 8px;
}

.note {
  flex-basis: 100%;
}

.note textarea {
  flex: 1;
  font: inherit;
}

.danger {
  color: #d92d20;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: #d92d20;
}
</style>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { ApiError, getOrder } from '@/api/admin'
import type { AdminOrder } from '@/types'
import { errorText } from '@/utils/error'
import { formatPrice } from '@/utils/discount'
import { cancelText, formatTime, STATUS_TEXT } from '@/utils/order'

const props = defineProps<{ token: string; orderNo: string }>()
const emit = defineEmits<{ close: []; unauthorized: [] }>()

const order = ref<AdminOrder | null>(null)
const error = ref('')
const box = ref<HTMLElement | null>(null)

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
        <p v-else-if="!order" class="hint">載入中</p>
        <template v-else>
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

.hint {
  color: var(--color-muted);
}

.error {
  color: #d92d20;
}
</style>

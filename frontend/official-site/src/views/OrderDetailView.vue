<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { cancelOrder, getOrder, payOrder } from '@/api/order'
import FormField from '@/components/FormField.vue'
import type { Order } from '@/types/order'
import { useCountdown } from '@/utils/countdown'
import { errorText } from '@/utils/error'
import { submitForm } from '@/utils/ecpayForm'
import { formatMoney, formatTwd } from '@/utils/format'
import { forgetOrderEmail, getOrderEmail, saveOrderEmail } from '@/utils/orderAccess'
import { isEmail } from '@/utils/validators'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const orderNo = computed(() => route.params.orderNo as string)

const email = ref(getOrderEmail(orderNo.value))
const emailInput = ref('')
const emailError = ref('')
const order = ref<Order | null>(null)
const loading = ref(false)
const loadError = ref('')

const paying = ref(false)
const payError = ref('')
const cancelling = ref(false)

const cancelText = computed(() => {
  switch (order.value?.cancelReason) {
    case 'EXPIRED':
      return t('order.cancelledExpired')
    case 'STAFF':
      return t('order.cancelledStaff')
    default:
      return t('order.cancelledCustomer')
  }
})

const pending = computed(() => order.value?.status === 'PENDING_PAYMENT')
const expiresAt = computed(() => (pending.value ? order.value?.expiresAt : undefined))
const countdown = useCountdown(expiresAt, () => load())
const expired = computed(() => pending.value && countdown.remainingMs.value <= 0)
const money = (v: number) => formatMoney(v, order.value?.currency ?? 'USD')

async function load() {
  if (!email.value) return
  loading.value = true
  loadError.value = ''
  try {
    order.value = await getOrder(orderNo.value, email.value)
  } catch (e) {
    order.value = null
    if (e instanceof ApiError && e.code === 'ORDER_NOT_FOUND') {
      // 信箱可能錯了，讓使用者重新輸入
      forgetOrderEmail(orderNo.value)
      email.value = ''
    }
    loadError.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function submitEmail() {
  if (!isEmail(emailInput.value)) {
    emailError.value = 'validation.email'
    return
  }
  emailError.value = ''
  email.value = emailInput.value.trim()
  saveOrderEmail(orderNo.value, email.value)
  load()
}

async function pay() {
  payError.value = ''
  if (!order.value) return
  paying.value = true
  let leaving = false
  try {
    const start = await payOrder(orderNo.value, email.value)
    if (start.mode === 'FAKE') {
      leaving = true
      await router.push({ name: 'payFake', query: { order: orderNo.value, trade: start.tradeNo } })
    } else if (start.action && start.fields) {
      // 送出後整頁跳轉，按鈕保持停用
      leaving = true
      submitForm(start.action, start.fields)
    }
  } catch (e) {
    payError.value = errorText(e)
    // 訂單狀態已變，重新取得
    if (e instanceof ApiError && ['ORDER_EXPIRED', 'ORDER_NOT_PAYABLE'].includes(e.code)) {
      load()
    }
  } finally {
    if (!leaving) paying.value = false
  }
}

async function cancel() {
  if (!order.value || !window.confirm(t('order.cancelConfirm'))) return
  cancelling.value = true
  payError.value = ''
  try {
    order.value = await cancelOrder(orderNo.value, email.value)
  } catch (e) {
    payError.value = errorText(e)
    load()
  } finally {
    cancelling.value = false
  }
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleString(locale.value)
}

watch(locale, load)
onMounted(load)
</script>

<template>
  <section>
    <h1>{{ t('order.title') }}</h1>

    <form v-if="!email" class="prompt" novalidate @submit.prevent="submitEmail">
      <p v-if="loadError" class="error" role="alert">{{ loadError }}</p>
      <p>{{ t('order.emailPrompt') }}</p>
      <FormField :label="t('lookup.email')" :error="emailError" for="o-email">
        <input id="o-email" v-model="emailInput" type="email" autocomplete="email" />
      </FormField>
      <button type="submit" class="primary">{{ t('order.view') }}</button>
    </form>

    <p v-else-if="loading && !order" class="hint">{{ t('order.loading') }}</p>
    <p v-else-if="loadError" class="error">{{ loadError }}</p>

    <template v-else-if="order">
      <div class="head">
        <div>
          {{ t('order.orderNo') }}
          <strong class="no">{{ order.orderNo }}</strong>
        </div>
        <span class="badge" :class="order.status">{{ t(`order.status.${order.status}`) }}</span>
      </div>

      <div v-if="order.status === 'PAID'" class="banner ok">
        <strong>{{ t('order.paidThanks') }}</strong>
        <div>{{ t('order.paidHint') }}</div>
        <div v-if="order.payment?.cardLast4" class="small">
          {{ t('payment.last4', { n: order.payment.cardLast4 }) }}
        </div>
      </div>
      <div v-else-if="order.status === 'SHIPPED' || order.status === 'COMPLETED'" class="banner ok">
        <strong>{{ order.status === 'SHIPPED' ? t('order.shippedThanks') : t('order.completedThanks') }}</strong>
        <div v-if="order.status === 'SHIPPED'">{{ t('order.shippedHint') }}</div>
        <div v-if="order.trackingNo" class="small">{{ t('order.trackingNo', { no: order.trackingNo }) }}</div>
      </div>
      <div v-else-if="order.status === 'CANCELLED'" class="banner off">
        {{ cancelText }}
      </div>

      <div class="layout">
        <div>
          <h2>{{ t('order.items') }}</h2>
          <ul class="items">
            <li v-for="i in order.items" :key="i.cardId">
              <img v-if="i.imageUrl" :src="i.imageUrl" :alt="i.cardName" referrerpolicy="no-referrer" />
              <div class="info">
                <div class="name">{{ i.cardName }}</div>
                <div v-if="i.cardNameEn !== i.cardName" class="small">{{ i.cardNameEn }}</div>
                <div class="small">{{ i.cardSetId }}</div>
              </div>
              <div class="qty">{{ money(i.unitPrice) }} × {{ i.quantity }}</div>
              <div class="sub">{{ money(i.subtotal) }}</div>
            </li>
          </ul>
          <dl class="amounts">
            <dt>{{ t('order.subtotal') }}</dt>
            <dd>{{ money(order.subtotal) }}</dd>
            <dt>{{ t('order.shippingFee') }}</dt>
            <dd>{{ order.shippingFee > 0 ? money(order.shippingFee) : t('order.free') }}</dd>
            <dt class="big">{{ t('order.total') }}</dt>
            <dd class="big">{{ money(order.total) }}</dd>
          </dl>

          <h2>{{ t('order.recipient') }}</h2>
          <p class="addr">
            {{ order.recipientName }} · {{ order.recipientPhone }}<br />
            {{ order.postalCode }} {{ order.city }}{{ order.address }}
          </p>
          <p class="small">{{ t('order.createdAt') }} {{ formatTime(order.createdAt) }}</p>
          <p v-if="order.paidAt" class="small">{{ t('order.paidAt') }} {{ formatTime(order.paidAt) }}</p>
        </div>

        <div v-if="pending" class="pay">
          <h2>{{ t('payment.title') }}</h2>
          <p class="timer" :class="{ late: expired }">
            {{ expired ? t('order.expired') : t('order.expiresIn', { time: countdown.label.value }) }}
          </p>
          <p v-if="order.totalTwd" class="twd">{{ formatTwd(order.totalTwd) }}</p>
          <p v-if="order.totalTwd && order.fxRate" class="small">
            {{ t('payment.rate', { rate: order.fxRate, amount: formatTwd(order.totalTwd) }) }}
          </p>
          <p v-if="order.payment?.status === 'FAILED'" class="error">{{ t('payment.lastFailed') }}</p>

          <p v-if="payError" class="error" role="alert">{{ payError }}</p>
          <button type="button" class="primary" :disabled="paying || cancelling || expired || !order.totalTwd" @click="pay">
            {{ paying ? t('payment.paying') : t('payment.pay', { amount: formatTwd(order.totalTwd ?? 0) }) }}
          </button>
          <p class="small">{{ t('payment.secure') }}</p>
          <button type="button" class="ghost" :disabled="paying || cancelling" @click="cancel">
            {{ t('order.cancel') }}
          </button>
        </div>
      </div>
    </template>

    <p class="foot"><RouterLink to="/orders">{{ t('lookup.title') }}</RouterLink></p>
  </section>
</template>

<style scoped>
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.no {
  margin-left: 6px;
  font-family: ui-monospace, monospace;
  font-size: 18px;
}

.badge {
  padding: 2px 12px;
  border-radius: 12px;
  background: #fef3c7;
  color: #92400e;
  font-size: 14px;
}

.badge.PAID,
.badge.SHIPPED,
.badge.COMPLETED {
  background: #dcfce7;
  color: #166534;
}

.badge.CANCELLED {
  background: #f1f1ee;
  color: var(--color-muted);
}

.banner {
  margin: 16px 0;
  padding: 14px 16px;
  border-radius: 8px;
}

.banner.ok {
  background: #dcfce7;
  color: #166534;
}

.banner.off {
  background: #f1f1ee;
  color: var(--color-muted);
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 32px;
  align-items: start;
}

.items {
  list-style: none;
  margin: 0;
  padding: 0;
}

.items li {
  display: grid;
  grid-template-columns: 48px 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
}

.items img {
  width: 48px;
  border-radius: 4px;
}

.name {
  font-weight: 600;
}

.small {
  font-size: 13px;
  color: var(--color-muted);
}

.sub {
  min-width: 90px;
  text-align: right;
  font-weight: 600;
}

.qty {
  font-size: 14px;
  color: var(--color-muted);
}

.amounts {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 16px;
  margin: 12px 0 24px;
}

.amounts dd {
  margin: 0;
  text-align: right;
}

.big {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}

.addr {
  margin: 0 0 8px;
}

.pay,
.prompt {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 16px;
}

.prompt {
  max-width: 420px;
}

.pay h2 {
  margin: 0;
  font-size: 18px;
}

.timer {
  margin: 0;
  color: #92400e;
}

.twd {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.timer.late {
  color: var(--color-primary);
}

.primary {
  height: 44px;
  border: 0;
  border-radius: 8px;
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.primary:disabled {
  background: #d4d4d0;
  cursor: not-allowed;
}

.ghost {
  height: 38px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  font: inherit;
  cursor: pointer;
}

.hint {
  color: var(--color-muted);
}

.error {
  margin: 0;
  color: var(--color-primary);
}

.foot {
  margin-top: 32px;
  font-size: 14px;
  color: var(--color-muted);
}

@media (max-width: 860px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .items li {
    grid-template-columns: 48px 1fr;
  }

  .qty,
  .sub {
    grid-column: 2;
    text-align: left;
  }
}
</style>

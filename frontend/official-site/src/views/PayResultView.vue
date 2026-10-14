<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import { getOrder } from '@/api/order'
import type { Order } from '@/types/order'
import { getOrderEmail } from '@/utils/orderAccess'

const MAX_TRIES = 6
const INTERVAL_MS = 2000

const { t } = useI18n()
const route = useRoute()
const orderNo = computed(() => String(route.query.order ?? ''))
const order = ref<Order | null>(null)
const checking = ref(false)
let timer: ReturnType<typeof setTimeout> | undefined
let tries = 0

const state = computed(() => {
  const o = order.value
  if (!o) return 'pending'
  if (o.status !== 'PENDING_PAYMENT') return 'paid'
  return o.payment?.status === 'FAILED' ? 'failed' : 'pending'
})

// 背景通知可能晚幾秒到，所以多查幾次
async function check() {
  const email = getOrderEmail(orderNo.value)
  if (!orderNo.value || !email) return
  checking.value = true
  try {
    order.value = await getOrder(orderNo.value, email)
  } catch {
    order.value = null
  } finally {
    checking.value = false
  }
  tries++
  if (state.value === 'pending' && tries < MAX_TRIES) {
    timer = setTimeout(check, INTERVAL_MS)
  }
}

function again() {
  tries = 0
  check()
}

onMounted(check)
onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <section>
    <h1>{{ t('payResult.title') }}</h1>
    <p v-if="!orderNo" class="hint">{{ t('payResult.noOrder') }}</p>
    <template v-else>
      <p v-if="state === 'paid'" class="ok" role="status">{{ t('payResult.paid') }}</p>
      <p v-else-if="state === 'failed'" class="error" role="alert">{{ t('payResult.failed') }}</p>
      <p v-else class="hint" role="status">
        {{ checking || tries < MAX_TRIES ? t('payResult.checking') : t('payResult.pending') }}
      </p>
      <p>
        <RouterLink :to="{ name: 'orderDetail', params: { orderNo } }">{{ t('payResult.viewOrder') }}</RouterLink>
        <button v-if="state === 'pending' && tries >= MAX_TRIES" type="button" class="link" @click="again">
          {{ t('payResult.refresh') }}
        </button>
      </p>
    </template>
  </section>
</template>

<style scoped>
.ok {
  color: #166534;
}

.error {
  color: var(--color-primary);
}

.hint {
  color: var(--color-muted);
}

.link {
  margin-left: 16px;
  border: 0;
  background: none;
  color: inherit;
  text-decoration: underline;
  cursor: pointer;
  font: inherit;
}
</style>

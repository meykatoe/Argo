<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { myOrders } from '@/api/auth'
import { ApiError } from '@/api/http'
import AppPagination from '@/components/AppPagination.vue'
import { useAuthStore } from '@/stores/auth'
import type { PageResult } from '@/types/card'
import type { Order } from '@/types/order'
import { errorText } from '@/utils/error'
import { formatMoney } from '@/utils/format'
import { saveOrderEmail } from '@/utils/orderAccess'

const { t, locale } = useI18n()
const router = useRouter()
const auth = useAuthStore()

const page = ref(1)
const data = ref<PageResult<Order> | null>(null)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await myOrders(page.value)
  } catch (e) {
    // 令牌失效就回登入頁
    if (e instanceof ApiError && e.status === 401) {
      await auth.logout()
      router.replace({ name: 'login', query: { redirect: '/account' } })
      return
    }
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function go(p: number) {
  page.value = p
  load()
}

// 訂單頁需要 Email，登入顧客不用再輸入
function open(o: Order) {
  saveOrderEmail(o.orderNo, o.customerEmail)
  router.push({ name: 'orderDetail', params: { orderNo: o.orderNo } })
}

async function logout() {
  await auth.logout()
  router.replace('/')
}

const time = (iso: string) => new Date(iso).toLocaleString(locale.value, { hour12: false })

onMounted(load)
</script>

<template>
  <section class="account">
    <div class="head">
      <div>
        <h1>{{ t('account.title') }}</h1>
        <p class="who">{{ auth.displayName }} · {{ auth.session?.email }}</p>
      </div>
      <button type="button" class="out" @click="logout">{{ t('nav.logout') }}</button>
    </div>

    <h2>{{ t('account.orders') }}</h2>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-else-if="loading && !data" class="hint">{{ t('account.loading') }}</p>
    <p v-else-if="data && data.items.length === 0" class="hint">{{ t('account.noOrders') }}</p>

    <ul v-if="data" class="orders">
      <li v-for="o in data.items" :key="o.orderNo">
        <button type="button" class="order" @click="open(o)">
          <span class="no">{{ o.orderNo }}</span>
          <span class="badge" :class="o.status">{{ t(`order.status.${o.status}`) }}</span>
          <span class="meta">{{ time(o.createdAt) }} · {{ t('account.itemCount', { n: o.items.length }) }}</span>
          <span class="total">{{ formatMoney(o.total, o.currency) }}</span>
        </button>
      </li>
    </ul>

    <AppPagination v-if="data" :page="data.page" :total-pages="data.totalPages" @change="go" />
  </section>
</template>

<style scoped>
.account {
  max-width: 720px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.who {
  margin: 0;
  color: var(--color-muted);
}

.out {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  font: inherit;
  cursor: pointer;
}

.orders {
  margin: 0 0 16px;
  padding: 0;
  list-style: none;
}

.order {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 12px;
  width: 100%;
  margin-bottom: 8px;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.order:hover {
  box-shadow: 0 2px 10px rgb(0 0 0 / 8%);
}

.no {
  font-weight: 600;
}

.badge {
  justify-self: end;
  padding: 0 8px;
  border-radius: 10px;
  background: #e5e7eb;
  font-size: 12px;
}

.badge.PENDING_PAYMENT {
  background: #fef3c7;
  color: #92400e;
}

.badge.PAID,
.badge.COMPLETED {
  background: #dcfce7;
  color: #15803d;
}

.meta {
  color: var(--color-muted);
  font-size: 13px;
}

.total {
  justify-self: end;
  font-weight: 600;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: #d92d20;
}
</style>

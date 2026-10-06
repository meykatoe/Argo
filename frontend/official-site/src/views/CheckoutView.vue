<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRouter } from 'vue-router'
import { getCardsByIds } from '@/api/card'
import { ApiError } from '@/api/http'
import { createOrder } from '@/api/order'
import FormField from '@/components/FormField.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import type { CardSummary } from '@/types/card'
import { TAIWAN_CITIES } from '@/utils/cities'
import { errorText } from '@/utils/error'
import { formatPrice, isAvailable } from '@/utils/format'
import { saveOrderEmail } from '@/utils/orderAccess'
import { isEmail, isPhone, isPostalCode } from '@/utils/validators'

const { t, locale } = useI18n()
const cart = useCartStore()
const auth = useAuthStore()
const router = useRouter()

const form = reactive({
  name: '',
  email: '',
  phone: '',
  sameAsBuyer: true,
  recipientName: '',
  recipientPhone: '',
  postalCode: '',
  city: '',
  address: '',
})
// 已登入就帶入姓名與 Email，仍可自行修改
if (auth.isLoggedIn && auth.session) {
  form.email = auth.session.email
  form.name = auth.session.name ?? ''
}
const errors = reactive<Record<string, string>>({})

const details = ref<Map<number, CardSummary>>(new Map())
const loaded = ref(false)
const loadError = ref('')
const submitting = ref(false)
const submitError = ref('')

const lines = computed(() =>
  cart.items.map((item) => {
    const card = details.value.get(item.id)
    const ok = !!card && isAvailable(card) && item.qty <= card.stock
    return { item, card, ok }
  }),
)
const blocked = computed(() => lines.value.some((l) => !l.ok))
const total = computed(
  () =>
    lines.value.reduce((sum, l) => sum + Math.round((l.card?.salePrice ?? 0) * 100) * l.item.qty, 0) /
    100,
)

async function load() {
  if (!cart.items.length) {
    loaded.value = true
    return
  }
  loadError.value = ''
  try {
    const list = await getCardsByIds(cart.items.map((i) => i.id))
    details.value = new Map(list.map((c) => [c.id, c]))
    loaded.value = true
  } catch (e) {
    loadError.value = errorText(e)
  }
}

function required(v: string) {
  return v.trim() ? '' : 'validation.required'
}

function validate(): boolean {
  errors.name = required(form.name)
  errors.email = isEmail(form.email) ? '' : 'validation.email'
  errors.phone = isPhone(form.phone) ? '' : 'validation.phone'
  errors.recipientName = form.sameAsBuyer ? '' : required(form.recipientName)
  errors.recipientPhone = form.sameAsBuyer ? '' : isPhone(form.recipientPhone) ? '' : 'validation.phone'
  errors.postalCode = isPostalCode(form.postalCode) ? '' : 'validation.postal'
  errors.city = required(form.city)
  errors.address = required(form.address)
  return Object.values(errors).every((e) => !e)
}

// 後端欄位名對應表單欄位
const SERVER_FIELDS: Record<string, string> = {
  'customer.name': 'name',
  'customer.email': 'email',
  'customer.phone': 'phone',
  'shipping.recipientName': 'recipientName',
  'shipping.recipientPhone': 'recipientPhone',
  'shipping.postalCode': 'postalCode',
  'shipping.city': 'city',
  'shipping.address': 'address',
}

async function submit() {
  submitError.value = ''
  if (!validate() || blocked.value) return
  submitting.value = true
  try {
    const order = await createOrder({
      items: cart.items.map((i) => ({ cardId: i.id, quantity: i.qty })),
      customer: { name: form.name.trim(), email: form.email.trim(), phone: form.phone.trim() },
      shipping: {
        recipientName: (form.sameAsBuyer ? form.name : form.recipientName).trim(),
        recipientPhone: (form.sameAsBuyer ? form.phone : form.recipientPhone).trim(),
        postalCode: form.postalCode.trim(),
        city: form.city,
        address: form.address.trim(),
      },
    })
    saveOrderEmail(order.orderNo, form.email.trim())
    // 商品已進訂單，清空購物車
    cart.clear()
    await router.replace({ name: 'orderDetail', params: { orderNo: order.orderNo } })
  } catch (e) {
    if (e instanceof ApiError && e.code === 'VALIDATION_ERROR') {
      for (const key of Object.keys(e.details)) {
        const field = SERVER_FIELDS[key]
        if (field) errors[field] = 'validation.required'
      }
    }
    submitError.value = errorText(e)
    // 庫存或商品變動，重新取得最新狀態
    if (e instanceof ApiError && ['INSUFFICIENT_STOCK', 'ITEM_UNAVAILABLE', 'ITEM_NOT_FOUND'].includes(e.code)) {
      load()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <h1>{{ t('checkout.title') }}</h1>

    <p v-if="loadError" class="error">{{ loadError }}</p>
    <div v-else-if="loaded && !cart.items.length" class="empty">
      <p class="hint">{{ t('cart.empty') }}</p>
      <RouterLink to="/cards" class="link">{{ t('cart.goShopping') }}</RouterLink>
    </div>
    <p v-else-if="!loaded" class="hint">{{ t('cart.loading') }}</p>
    <div v-else class="layout">
      <form class="form" novalidate @submit.prevent="submit">
        <p v-if="auth.isLoggedIn" class="note">{{ t('checkout.loggedInNote') }}</p>
        <p v-else class="note">
          {{ t('checkout.guestNote') }}
          <RouterLink :to="{ name: 'login', query: { redirect: '/checkout' } }" class="link">
            {{ t('nav.login') }}
          </RouterLink>
        </p>
        <fieldset>
          <legend>{{ t('checkout.buyer') }}</legend>
          <FormField :label="t('checkout.name')" :error="errors.name" for="c-name">
            <input id="c-name" v-model="form.name" autocomplete="name" />
          </FormField>
          <FormField
            :label="t('checkout.email')"
            :error="errors.email"
            :hint="t('checkout.emailHint')"
            for="c-email"
          >
            <input id="c-email" v-model="form.email" type="email" autocomplete="email" />
          </FormField>
          <FormField :label="t('checkout.phone')" :error="errors.phone" for="c-phone">
            <input id="c-phone" v-model="form.phone" type="tel" autocomplete="tel" />
          </FormField>
        </fieldset>

        <fieldset>
          <legend>{{ t('checkout.shipping') }}</legend>
          <label class="check">
            <input v-model="form.sameAsBuyer" type="checkbox" />
            {{ t('checkout.sameAsBuyer') }}
          </label>
          <template v-if="!form.sameAsBuyer">
            <FormField :label="t('checkout.recipientName')" :error="errors.recipientName" for="c-rname">
              <input id="c-rname" v-model="form.recipientName" />
            </FormField>
            <FormField :label="t('checkout.recipientPhone')" :error="errors.recipientPhone" for="c-rphone">
              <input id="c-rphone" v-model="form.recipientPhone" type="tel" />
            </FormField>
          </template>
          <div class="row">
            <FormField :label="t('checkout.postalCode')" :error="errors.postalCode" for="c-postal">
              <input id="c-postal" v-model="form.postalCode" inputmode="numeric" autocomplete="postal-code" />
            </FormField>
            <FormField :label="t('checkout.city')" :error="errors.city" for="c-city">
              <select id="c-city" v-model="form.city">
                <option value="" disabled>{{ t('checkout.cityPlaceholder') }}</option>
                <option v-for="c in TAIWAN_CITIES" :key="c" :value="c">{{ c }}</option>
              </select>
            </FormField>
          </div>
          <FormField :label="t('checkout.address')" :error="errors.address" for="c-address">
            <input id="c-address" v-model="form.address" autocomplete="street-address" />
          </FormField>
        </fieldset>

        <p v-if="blocked" class="error">{{ t('checkout.blocked') }}</p>
        <p v-if="submitError" class="error" role="alert">{{ submitError }}</p>
        <div class="actions">
          <RouterLink to="/cart" class="link">{{ t('checkout.backToCart') }}</RouterLink>
          <button type="submit" class="primary" :disabled="submitting || blocked">
            {{ submitting ? t('checkout.submitting') : t('checkout.submit') }}
          </button>
        </div>
      </form>

      <aside class="summary">
        <h2>{{ t('checkout.summary') }}</h2>
        <ul>
          <li v-for="l in lines" :key="l.item.id" :class="{ off: !l.ok }">
            <span class="n">{{ l.card?.cardName ?? '—' }} × {{ l.item.qty }}</span>
            <span>{{ l.card ? formatPrice(l.card.salePrice * l.item.qty) : '' }}</span>
          </li>
        </ul>
        <div class="total">
          <span>{{ t('order.total') }}</span>
          <strong>{{ formatPrice(total) }}</strong>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 32px;
  align-items: start;
}

fieldset {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
  padding: 16px;
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

legend {
  padding: 0 6px;
  font-weight: 600;
}

.row {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 12px;
}

.check {
  display: flex;
  align-items: center;
  gap: 6px;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.primary {
  height: 44px;
  padding: 0 28px;
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

.link {
  color: var(--color-muted);
}

.summary {
  position: sticky;
  top: 16px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 16px;
}

.summary h2 {
  margin: 0 0 8px;
  font-size: 18px;
}

.summary ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.summary li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  font-size: 14px;
}

.summary li.off {
  color: var(--color-primary);
}

.n {
  overflow: hidden;
  text-overflow: ellipsis;
}

.total {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
  font-size: 18px;
}

.total strong {
  color: var(--color-primary);
}

.hint {
  color: var(--color-muted);
}

.error {
  color: var(--color-primary);
}

@media (max-width: 860px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .summary {
    position: static;
    order: -1;
  }
}
.note {
  margin: 0;
  color: var(--color-muted);
  font-size: 14px;
}
</style>

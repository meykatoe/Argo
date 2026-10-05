<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { getCardsByIds } from '@/api/card'
import { useCartStore } from '@/stores/cart'
import type { CardSummary } from '@/types/card'
import { errorText } from '@/utils/error'
import { formatPrice, isAvailable } from '@/utils/format'

const { t, locale } = useI18n()
const cart = useCartStore()

const details = ref<Map<number, CardSummary>>(new Map())
const notices = ref<Record<number, string>>({})
const loaded = ref(false)
const loading = ref(false)
const error = ref('')

type Status = 'ok' | 'soldOut' | 'gone'

const lines = computed(() =>
  cart.items.map((item) => {
    const card = details.value.get(item.id)
    let status: Status = 'ok'
    if (loaded.value && !card) status = 'gone'
    else if (card && !isAvailable(card)) status = 'soldOut'
    return { item, card, status }
  }),
)

const payable = computed(() => lines.value.filter((l) => l.status === 'ok' && l.card))

// 以分計算避免浮點誤差
const total = computed(
  () =>
    payable.value.reduce((sum, l) => sum + Math.round(l.card!.salePrice * 100) * l.item.qty, 0) /
    100,
)
const payableQty = computed(() => payable.value.reduce((sum, l) => sum + l.item.qty, 0))

async function load() {
  if (!cart.items.length) {
    details.value = new Map()
    loaded.value = true
    return
  }
  loading.value = true
  error.value = ''
  try {
    const list = await getCardsByIds(cart.items.map((i) => i.id))
    details.value = new Map(list.map((c) => [c.id, c]))
    loaded.value = true
    // 數量超過庫存就降到庫存
    for (const item of [...cart.items]) {
      const card = details.value.get(item.id)
      if (card && isAvailable(card) && item.qty > card.stock) {
        cart.setQty(item.id, card.stock)
        notices.value[item.id] = t('cart.adjusted', { n: card.stock })
      }
    }
  } catch (e) {
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function change(id: number, delta: number) {
  cart.setQty(id, cart.qtyOf(id) + delta)
  delete notices.value[id]
}

function remove(id: number) {
  cart.remove(id)
  delete notices.value[id]
}

watch(locale, load)
onMounted(load)
</script>

<template>
  <section>
    <h1>{{ t('cart.title') }}</h1>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="loading && !loaded" class="hint">{{ t('cart.loading') }}</p>
    <div v-else-if="!cart.items.length" class="empty">
      <p class="hint">{{ t('cart.empty') }}</p>
      <RouterLink to="/cards" class="go">{{ t('cart.goShopping') }}</RouterLink>
    </div>
    <template v-else>
      <ul class="lines">
        <li v-for="l in lines" :key="l.item.id" class="line" :class="{ off: l.status !== 'ok' }">
          <div class="thumb">
            <img
              v-if="l.card?.imageUrl"
              :src="l.card.imageUrl"
              :alt="l.card.cardName"
              referrerpolicy="no-referrer"
            />
          </div>
          <div class="info">
            <RouterLink v-if="l.card" :to="`/cards/${l.card.id}`" class="name">
              {{ l.card.cardName }}
            </RouterLink>
            <div v-if="l.card" class="code">{{ l.card.cardSetId }} · {{ l.card.rarity }}</div>
            <div v-if="l.status === 'gone'" class="warn">{{ t('cart.gone') }}</div>
            <div v-else-if="l.status === 'soldOut'" class="warn">{{ t('cart.unavailable') }}</div>
            <div v-if="notices[l.item.id]" class="warn">{{ notices[l.item.id] }}</div>
          </div>
          <div v-if="l.status === 'ok' && l.card" class="qty" :aria-label="t('cart.qty')">
            <button
              type="button"
              :aria-label="t('cart.decrease')"
              :disabled="l.item.qty <= 1"
              @click="change(l.item.id, -1)"
            >
              −
            </button>
            <span class="num">{{ l.item.qty }}</span>
            <button
              type="button"
              :aria-label="t('cart.increase')"
              :disabled="l.item.qty >= l.card.stock"
              @click="change(l.item.id, 1)"
            >
              +
            </button>
          </div>
          <div v-if="l.status === 'ok' && l.card" class="sum">
            <div class="unit">{{ formatPrice(l.card.salePrice) }}</div>
            <div class="subtotal">
              {{ t('cart.subtotal') }} {{ formatPrice(l.card.salePrice * l.item.qty) }}
            </div>
          </div>
          <button type="button" class="remove" @click="remove(l.item.id)">
            {{ t('cart.remove') }}
          </button>
        </li>
      </ul>

      <div class="summary">
        <div class="count">{{ t('cart.items', { n: payableQty }) }}</div>
        <div class="total">{{ t('cart.total') }} {{ formatPrice(total) }}</div>
        <RouterLink v-if="payable.length" to="/checkout" class="checkout">
          {{ t('cart.checkout') }}
        </RouterLink>
        <button v-else type="button" class="checkout" disabled>{{ t('cart.checkout') }}</button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.hint {
  color: var(--color-muted);
}

.error {
  color: var(--color-primary);
}

.empty {
  padding: 32px 0;
}

.go {
  display: inline-block;
  padding: 8px 18px;
  border-radius: 6px;
  background: var(--color-primary);
  color: #fff;
}

.lines {
  list-style: none;
  margin: 0;
  padding: 0;
}

.line {
  display: grid;
  grid-template-columns: 64px 1fr auto auto auto;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid var(--color-border);
}

.line.off .thumb img {
  opacity: 0.4;
}

.thumb {
  aspect-ratio: 63 / 88;
  background: #f1f1ee;
  border-radius: 4px;
  overflow: hidden;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.name {
  font-weight: 600;
}

.code {
  font-size: 12px;
  color: var(--color-muted);
}

.warn {
  margin-top: 4px;
  font-size: 13px;
  color: var(--color-primary);
}

.qty {
  display: flex;
  align-items: center;
  gap: 4px;
}

.qty button,
.remove {
  height: 32px;
  min-width: 32px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  font: inherit;
  cursor: pointer;
}

.qty button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.num {
  min-width: 32px;
  text-align: center;
}

.sum {
  text-align: right;
}

.unit {
  font-size: 13px;
  color: var(--color-muted);
}

.subtotal {
  font-weight: 600;
  color: var(--color-primary);
}

.summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  padding: 20px 0;
}

.total {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
}

.checkout {
  display: inline-flex;
  align-items: center;
  height: 42px;
  padding: 0 24px;
  border: 0;
  border-radius: 8px;
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.checkout:disabled {
  background: #d4d4d0;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .line {
    grid-template-columns: 56px 1fr;
  }

  .qty,
  .sum,
  .remove {
    grid-column: 2;
    justify-self: start;
  }
}
</style>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCartStore } from '@/stores/cart'
import type { Flag } from '@/types/card'
import { isAvailable, isOffShelf } from '@/utils/format'

const props = defineProps<{ card: { id: number; stock: number; salePrice: number; onSale: Flag } }>()

const { t } = useI18n()
const cart = useCartStore()
const justAdded = ref(false)
let timer: ReturnType<typeof setTimeout> | undefined

const inCart = computed(() => cart.qtyOf(props.card.id))
const soldOut = computed(() => !isAvailable(props.card))
const atLimit = computed(() => inCart.value >= props.card.stock)

const label = computed(() => {
  if (isOffShelf(props.card)) return t('detail.offShelf')
  if (soldOut.value) return t('detail.outOfStock')
  if (justAdded.value) return t('cart.added')
  if (atLimit.value) return t('cart.limitReached')
  return t('cart.add')
})

function add() {
  cart.add(props.card.id)
  justAdded.value = true
  clearTimeout(timer)
  timer = setTimeout(() => (justAdded.value = false), 1200)
}

onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <button
    type="button"
    class="add"
    :class="{ done: justAdded }"
    :disabled="soldOut || (atLimit && !justAdded)"
    @click="add"
  >
    {{ label }}
  </button>
</template>

<style scoped>
.add {
  height: 36px;
  padding: 0 14px;
  border: 1px solid var(--color-primary);
  border-radius: 6px;
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.add.done {
  background: #166534;
  border-color: #166534;
}

.add:disabled:not(.done) {
  background: #f1f1ee;
  border-color: var(--color-border);
  color: var(--color-muted);
  cursor: not-allowed;
}
</style>

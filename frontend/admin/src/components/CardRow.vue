<script setup lang="ts">
import { ref, watch } from 'vue'
import { ApiError, setExtraDiscount } from '@/api/admin'
import type { AdminCard } from '@/types'
import CardImageModal from './CardImageModal.vue'
import { discountLabel, formatPrice, parseDiscount } from '@/utils/discount'
import { errorText } from '@/utils/error'

const props = defineProps<{ card: AdminCard; token: string }>()
const emit = defineEmits<{ saved: [card: AdminCard]; unauthorized: [] }>()

const input = ref(String(props.card.extraDiscount))
const saving = ref(false)
const error = ref('')
const showImage = ref(false)

watch(
  () => props.card.extraDiscount,
  (v) => {
    input.value = String(v)
  },
)

async function save() {
  const value = parseDiscount(input.value)
  if (value === null) {
    error.value = '請輸入 0 到 1 之間的數字，例如 0.4'
    return
  }
  saving.value = true
  error.value = ''
  try {
    emit('saved', await setExtraDiscount(props.token, props.card.id, value))
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('unauthorized')
    }
    error.value = errorText(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <tr>
    <td class="code">{{ card.cardSetId }}</td>
    <td>
      {{ card.cardName }}
      <div>
        <button type="button" class="info" @click="showImage = true">卡牌資訊</button>
      </div>
      <CardImageModal
        v-if="showImage"
        :src="card.imageUrl"
        :name="card.cardName"
        @close="showImage = false"
      />
    </td>
    <td class="num">{{ formatPrice(card.marketPrice) }}</td>
    <td class="num">{{ formatPrice(card.listPrice) }}</td>
    <td>
      <input
        v-model="input"
        class="discount"
        inputmode="decimal"
        :disabled="card.priceOverridden || saving"
        :aria-label="`${card.cardSetId} 額外折扣`"
        @keyup.enter="save"
      />
      <span v-if="parseDiscount(input) !== null" class="hint">
        {{ discountLabel(parseDiscount(input)!) }}
      </span>
      <div v-if="card.priceOverridden" class="hint">手動定價，不套用折扣</div>
      <div v-if="error" class="error">{{ error }}</div>
    </td>
    <td class="num" :class="{ sale: card.extraDiscount < 1 }">{{ formatPrice(card.salePrice) }}</td>
    <td>
      <button type="button" class="primary" :disabled="card.priceOverridden || saving" @click="save">
        儲存
      </button>
    </td>
  </tr>
</template>

<style scoped>
td {
  padding: 8px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: top;
}

.info {
  margin-top: 4px;
  padding: 1px 8px;
  border-color: transparent;
  background: #e5e7eb;
  color: var(--color-muted);
  font-size: 12px;
}

.info:hover {
  background: #d1d5db;
  color: var(--color-text);
}

.code {
  white-space: nowrap;
  color: var(--color-muted);
}

.num {
  text-align: right;
  white-space: nowrap;
}

.sale {
  color: var(--color-primary);
  font-weight: 700;
}

.discount {
  width: 80px;
}

.hint {
  margin-left: 6px;
  font-size: 12px;
  color: var(--color-muted);
}

.error {
  font-size: 12px;
  color: #d92d20;
}
</style>

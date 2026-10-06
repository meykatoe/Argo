<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ApiError, setCardSetDiscount, setCardSetOnSale } from '@/api/admin'
import type { AdminCardSet, Flag } from '@/types'
import { categoryName } from '@/utils/category'
import { discountLabel, discountRangeLabel, parseDiscount } from '@/utils/discount'
import { errorText } from '@/utils/error'

const props = defineProps<{ set: AdminCardSet; token: string }>()
const emit = defineEmits<{ saved: [set: AdminCardSet]; unauthorized: [] }>()

const input = ref('')
const busy = ref(false)
const error = ref('')

async function run(job: () => Promise<AdminCardSet>) {
  busy.value = true
  error.value = ''
  try {
    emit('saved', await job())
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('unauthorized')
    }
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}

function toggle() {
  const next: Flag = props.set.onSale === 1 ? 0 : 1
  const name = props.set.setName
  const msg =
    next === 0
      ? `確定要下架「${name}」嗎？\n下架後官網仍會顯示這個系列的卡片，但顧客無法購買。`
      : `確定要重新上架「${name}」嗎？`
  if (!window.confirm(msg)) {
    return
  }
  run(() => setCardSetOnSale(props.token, props.set.setId, next))
}

function applyDiscount() {
  const value = parseDiscount(input.value)
  if (value === null) {
    error.value = '請輸入 0 到 1 之間的數字，例如 0.4'
    return
  }
  const msg =
    `將對「${props.set.setName}」全部 ${props.set.cardCount} 張卡設定額外折扣 ${value}（${discountLabel(value)}）。\n` +
    '已手動定價的卡片只記錄折扣，價格不變。確定嗎？'
  if (!window.confirm(msg)) {
    return
  }
  run(async () => {
    const updated = await setCardSetDiscount(props.token, props.set.setId, value)
    input.value = ''
    return updated
  })
}
</script>

<template>
  <tr :class="{ off: set.onSale === 0 }">
    <td class="code">{{ set.setId }}</td>
    <td>
      {{ set.setName }}
      <div v-if="set.setNameEn !== set.setName" class="en">{{ set.setNameEn }}</div>
    </td>
    <td>{{ categoryName(set.category) }}</td>
    <td class="num">{{ set.cardCount }}</td>
    <td>
      <span class="state" :class="set.onSale === 1 ? 'on' : 'down'">
        {{ set.onSale === 1 ? '上架中' : '已下架' }}
      </span>
      <div>
        <button type="button" class="small" :disabled="busy" @click="toggle">
          {{ set.onSale === 1 ? '下架' : '上架' }}
        </button>
      </div>
    </td>
    <td>
      <div class="range">{{ discountRangeLabel(set.minDiscount, set.maxDiscount) }}</div>
      <input
        v-model="input"
        class="discount"
        inputmode="decimal"
        placeholder="例如 0.4"
        :disabled="busy"
        :aria-label="`${set.setId} 整個系列折扣`"
        @keyup.enter="applyDiscount"
      />
      <button type="button" class="small primary" :disabled="busy" @click="applyDiscount">
        套用整個系列
      </button>
      <div v-if="error" class="error" role="alert">{{ error }}</div>
    </td>
    <td>
      <RouterLink :to="{ path: '/cards', query: { setId: set.setId } }" class="link">查看卡片</RouterLink>
    </td>
  </tr>
</template>

<style scoped>
td {
  padding: 8px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: top;
}

.off {
  background: #f6f6f3;
}

.code {
  white-space: nowrap;
  color: var(--color-muted);
}

.num {
  text-align: right;
}

.en {
  font-size: 12px;
  color: var(--color-muted);
}

.state {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.state.on {
  background: #dcfce7;
  color: #15803d;
}

.state.down {
  background: #e5e7eb;
  color: var(--color-muted);
}

.small {
  margin-top: 4px;
  padding: 2px 10px;
  font-size: 12px;
}

.range {
  margin-bottom: 4px;
  font-size: 13px;
}

.discount {
  width: 90px;
}

.link {
  color: var(--color-primary);
  white-space: nowrap;
}

.error {
  font-size: 12px;
  color: #d92d20;
}
</style>

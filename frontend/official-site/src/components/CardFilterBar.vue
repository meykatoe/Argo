<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { CardSearchParams, CardSet } from '@/types/card'

const props = defineProps<{ modelValue: CardSearchParams; sets: CardSet[] }>()
const emit = defineEmits<{ change: [patch: CardSearchParams] }>()

const COLORS = ['Red', 'Green', 'Blue', 'Purple', 'Black', 'Yellow']
const RARITIES = ['L', 'C', 'UC', 'R', 'SR', 'SEC', 'TR', 'P', 'PR']
const TYPES = ['Leader', 'Character', 'Event', 'Stage']
const CATEGORIES = [
  { value: 'booster', label: '補充包' },
  { value: 'starter', label: '起始牌組' },
  { value: 'promo', label: '促銷卡' },
]
const SORTS = [
  { value: 'cardSetId', label: '編號' },
  { value: 'cardName', label: '名稱' },
  { value: 'marketPrice', label: '價格' },
]

const keyword = ref(props.modelValue.keyword ?? '')
watch(
  () => props.modelValue.keyword,
  (v) => (keyword.value = v ?? ''),
)

// 選了類別就只列該類系列
const visibleSets = computed(() =>
  props.modelValue.category
    ? props.sets.filter((s) => s.category === props.modelValue.category)
    : props.sets,
)

function pick(key: keyof CardSearchParams, e: Event) {
  const value = (e.target as HTMLSelectElement).value
  // 換類別時清掉系列
  const patch: CardSearchParams = { [key]: value || undefined }
  if (key === 'category') patch.setId = undefined
  emit('change', patch)
}

function submit() {
  emit('change', { keyword: keyword.value.trim() })
}
</script>

<template>
  <form class="bar" @submit.prevent="submit">
    <input v-model="keyword" type="search" placeholder="搜尋卡名或編號" />
    <select :value="modelValue.category ?? ''" @change="pick('category', $event)">
      <option value="">全部類別</option>
      <option v-for="c in CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
    </select>
    <select :value="modelValue.setId ?? ''" @change="pick('setId', $event)">
      <option value="">全部系列</option>
      <option v-for="s in visibleSets" :key="s.setId" :value="s.setId">
        {{ s.setId }} {{ s.setName }}
      </option>
    </select>
    <select :value="modelValue.color ?? ''" @change="pick('color', $event)">
      <option value="">全部顏色</option>
      <option v-for="c in COLORS" :key="c" :value="c">{{ c }}</option>
    </select>
    <select :value="modelValue.rarity ?? ''" @change="pick('rarity', $event)">
      <option value="">全部稀有度</option>
      <option v-for="r in RARITIES" :key="r" :value="r">{{ r }}</option>
    </select>
    <select :value="modelValue.cardType ?? ''" @change="pick('cardType', $event)">
      <option value="">全部種類</option>
      <option v-for="t in TYPES" :key="t" :value="t">{{ t }}</option>
    </select>
    <select :value="modelValue.sortBy" @change="pick('sortBy', $event)">
      <option v-for="s in SORTS" :key="s.value" :value="s.value">依{{ s.label }}排序</option>
    </select>
    <button type="button" class="dir" @click="emit('change', { desc: !modelValue.desc })">
      {{ modelValue.desc ? '降冪' : '升冪' }}
    </button>
    <button type="submit" class="go">搜尋</button>
  </form>
</template>

<style scoped>
.bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

input,
select,
button {
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  font: inherit;
}

input {
  flex: 1 1 200px;
}

button {
  cursor: pointer;
}

.go {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
</style>

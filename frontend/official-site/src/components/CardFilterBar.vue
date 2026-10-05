<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CardSearchParams, CardSet } from '@/types/card'

const props = defineProps<{ modelValue: CardSearchParams; sets: CardSet[] }>()
const emit = defineEmits<{ change: [patch: CardSearchParams] }>()

const { t } = useI18n()

const COLORS = ['Red', 'Green', 'Blue', 'Purple', 'Black', 'Yellow']
const RARITIES = ['L', 'C', 'UC', 'R', 'SR', 'SEC', 'TR', 'P', 'PR']
const TYPES = ['Leader', 'Character', 'Event', 'Stage']
const CATEGORIES = ['booster', 'starter', 'promo']
const SORTS = ['cardSetId', 'cardName', 'marketPrice']

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
    <input v-model="keyword" type="search" :placeholder="t('filter.keyword')" />
    <select :value="modelValue.category ?? ''" @change="pick('category', $event)">
      <option value="">{{ t('filter.allCategories') }}</option>
      <option v-for="c in CATEGORIES" :key="c" :value="c">{{ t(`categories.${c}`) }}</option>
    </select>
    <select :value="modelValue.setId ?? ''" @change="pick('setId', $event)">
      <option value="">{{ t('filter.allSets') }}</option>
      <option v-for="s in visibleSets" :key="s.setId" :value="s.setId">
        {{ s.setId }} {{ s.setName }}
      </option>
    </select>
    <select :value="modelValue.color ?? ''" @change="pick('color', $event)">
      <option value="">{{ t('filter.allColors') }}</option>
      <option v-for="c in COLORS" :key="c" :value="c">{{ t(`color.${c}`) }}</option>
    </select>
    <select :value="modelValue.rarity ?? ''" @change="pick('rarity', $event)">
      <option value="">{{ t('filter.allRarities') }}</option>
      <option v-for="r in RARITIES" :key="r" :value="r">{{ r }}</option>
    </select>
    <select :value="modelValue.cardType ?? ''" @change="pick('cardType', $event)">
      <option value="">{{ t('filter.allTypes') }}</option>
      <option v-for="ty in TYPES" :key="ty" :value="ty">{{ t(`cardType.${ty}`) }}</option>
    </select>
    <select :value="modelValue.sortBy" @change="pick('sortBy', $event)">
      <option v-for="s in SORTS" :key="s" :value="s">{{ t(`filter.sort.${s}`) }}</option>
    </select>
    <button type="button" class="dir" @click="emit('change', { desc: !modelValue.desc })">
      {{ modelValue.desc ? t('filter.desc') : t('filter.asc') }}
    </button>
    <button type="submit" class="go">{{ t('filter.search') }}</button>
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

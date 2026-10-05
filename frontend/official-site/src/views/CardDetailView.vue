<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getCard } from '@/api/card'
import { ApiError } from '@/api/http'
import type { CardDetail } from '@/types/card'
import { formatPrice } from '@/utils/format'

const route = useRoute()
const card = ref<CardDetail | null>(null)
const loading = ref(true)
const error = ref('')

async function load(id: string) {
  loading.value = true
  error.value = ''
  card.value = null
  try {
    card.value = await getCard(Number(id))
  } catch (e) {
    error.value = e instanceof ApiError && e.status === 404 ? '找不到這張卡片' : '載入失敗'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id as string, load, { immediate: true })

// 無值的欄位不顯示
function rows(c: CardDetail): [string, string | number | null][] {
  return [
    ['編號', c.cardSetId],
    ['系列', c.setName ? `${c.setId} ${c.setName}` : c.setId],
    ['稀有度', c.rarity],
    ['種類', c.cardType],
    ['顏色', c.cardColor],
    ['費用', c.cardCost],
    ['力量', c.cardPower],
    ['生命', c.life],
    ['Counter', c.counterAmount],
    ['屬性', c.attribute],
    ['特徵', c.subTypes],
  ]
}
</script>

<template>
  <section>
    <RouterLink to="/cards" class="back">← 返回列表</RouterLink>

    <p v-if="loading" class="hint">載入中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <div v-else-if="card" class="detail">
      <div class="image">
        <img
          v-if="card.imageUrl"
          :src="card.imageUrl"
          :alt="card.cardName"
          referrerpolicy="no-referrer"
        />
        <span v-else class="hint">無圖片</span>
      </div>
      <div class="body">
        <h1>{{ card.cardName }}</h1>
        <p class="price">參考市價 {{ formatPrice(card.marketPrice) }}</p>
        <dl>
          <template v-for="[label, value] in rows(card)" :key="label">
            <template v-if="value !== null && value !== ''">
              <dt>{{ label }}</dt>
              <dd>{{ value }}</dd>
            </template>
          </template>
        </dl>
        <p v-if="card.cardText" class="text">{{ card.cardText }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.back {
  color: var(--color-muted);
}

.detail {
  display: grid;
  grid-template-columns: minmax(240px, 360px) 1fr;
  gap: 32px;
  margin-top: 16px;
}

.image img {
  width: 100%;
  border-radius: 12px;
}

h1 {
  margin-top: 0;
}

.price {
  color: var(--color-primary);
  font-size: 20px;
  font-weight: 700;
}

dl {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 6px 12px;
}

dt {
  color: var(--color-muted);
}

dd {
  margin: 0;
}

.text {
  white-space: pre-line;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 12px 14px;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: var(--color-primary);
}

@media (max-width: 720px) {
  .detail {
    grid-template-columns: 1fr;
  }
}
</style>

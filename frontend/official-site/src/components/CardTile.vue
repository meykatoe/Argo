<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { CardSummary } from '@/types/card'
import { formatPrice } from '@/utils/format'

defineProps<{ card: CardSummary }>()
</script>

<template>
  <RouterLink :to="`/cards/${card.id}`" class="tile">
    <div class="image">
      <img
        v-if="card.imageUrl"
        :src="card.imageUrl"
        :alt="card.cardName"
        loading="lazy"
        referrerpolicy="no-referrer"
      />
      <span v-else class="empty">無圖片</span>
    </div>
    <div class="info">
      <div class="code">{{ card.cardSetId }} · {{ card.rarity }}</div>
      <div class="name" :title="card.cardName">{{ card.cardName }}</div>
      <div class="price">{{ formatPrice(card.marketPrice) }}</div>
    </div>
  </RouterLink>
</template>

<style scoped>
.tile {
  display: block;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.15s;
}

.tile:hover {
  box-shadow: 0 4px 14px rgb(0 0 0 / 12%);
}

.image {
  aspect-ratio: 63 / 88;
  background: #f1f1ee;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.empty {
  color: var(--color-muted);
  font-size: 14px;
}

.info {
  padding: 8px 10px 10px;
}

.code {
  font-size: 12px;
  color: var(--color-muted);
}

.name {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.price {
  margin-top: 4px;
  color: var(--color-primary);
  font-weight: 600;
}
</style>

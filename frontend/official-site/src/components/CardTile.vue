<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import type { CardSummary } from '@/types/card'
import { isAvailable, isOffShelf } from '@/utils/format'
import AddToCartButton from './AddToCartButton.vue'
import PriceTag from './PriceTag.vue'

defineProps<{ card: CardSummary }>()

const { t } = useI18n()
</script>

<template>
  <article class="tile">
    <RouterLink :to="`/cards/${card.id}`" class="link">
    <div class="image" :class="{ sold: !isAvailable(card) }">
      <img
        v-if="card.imageUrl"
        :src="card.imageUrl"
        :alt="card.cardName"
        loading="lazy"
        referrerpolicy="no-referrer"
      />
      <span v-else class="empty">{{ t('cards.noImage') }}</span>
      <span v-if="!isAvailable(card)" class="badge">
        {{ isOffShelf(card) ? t('detail.offShelf') : t('detail.outOfStock') }}
      </span>
    </div>
    <div class="info">
      <div class="code">{{ card.cardSetId }} · {{ card.rarity }}</div>
      <div class="name" :title="card.cardName">{{ card.cardName }}</div>
      <div v-if="card.cardNameEn !== card.cardName" class="en" :title="card.cardNameEn">
        {{ card.cardNameEn }}
      </div>
      <div v-if="card.salePrice > 0" class="price">
        <PriceTag :sale-price="card.salePrice" :list-price="card.listPrice" />
      </div>
      <div v-else class="price unset">{{ t('detail.unpriced') }}</div>
    </div>
    </RouterLink>
    <div class="actions">
      <AddToCartButton :card="card" />
    </div>
  </article>
</template>

<style scoped>
.link {
  display: block;
}

.actions {
  padding: 0 10px 10px;
}

.actions :deep(.add) {
  width: 100%;
}

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
  position: relative;
  aspect-ratio: 63 / 88;
  background: #f1f1ee;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image.sold img {
  opacity: 0.45;
}

.badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #1f2430;
  color: #fff;
  font-size: 12px;
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

.en {
  font-size: 12px;
  color: var(--color-muted);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.price {
  margin-top: 4px;
  color: var(--color-primary);
  font-weight: 600;
}

.price.unset {
  color: var(--color-muted);
  font-weight: 400;
}
</style>

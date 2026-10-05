<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import { getCard } from '@/api/card'
import type { CardDetail } from '@/types/card'
import { errorText } from '@/utils/error'
import { colorText, formatPrice, typeText } from '@/utils/format'

const route = useRoute()
const { t, locale } = useI18n()
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
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id as string, load, { immediate: true })

// 切換語言重新取資料
watch(locale, () => load(route.params.id as string))

// 無值的欄位不顯示
function rows(c: CardDetail): [string, string | number | null][] {
  return [
    [t('detail.rows.number'), c.cardSetId],
    [t('detail.rows.set'), c.setName ? `${c.setId} ${c.setName}` : c.setId],
    [t('detail.rows.rarity'), c.rarity],
    [t('detail.rows.type'), typeText(c.cardType)],
    [t('detail.rows.color'), colorText(c.cardColor)],
    [t('detail.rows.cost'), c.cardCost],
    [t('detail.rows.power'), c.cardPower],
    [t('detail.rows.life'), c.life],
    [t('detail.rows.counter'), c.counterAmount],
    [t('detail.rows.attribute'), c.attribute],
    [t('detail.rows.features'), c.subTypes],
  ]
}
</script>

<template>
  <section>
    <RouterLink to="/cards" class="back">{{ t('cards.back') }}</RouterLink>

    <p v-if="loading" class="hint">{{ t('cards.loading') }}</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <div v-else-if="card" class="detail">
      <div class="image">
        <img
          v-if="card.imageUrl"
          :src="card.imageUrl"
          :alt="card.cardName"
          referrerpolicy="no-referrer"
        />
        <span v-else class="hint">{{ t('cards.noImage') }}</span>
      </div>
      <div class="body">
        <h1>{{ card.cardName }}</h1>
        <p v-if="card.cardNameEn !== card.cardName" class="en">{{ card.cardNameEn }}</p>
        <p class="price">{{ t('detail.price') }} {{ formatPrice(card.marketPrice) }}</p>
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
  margin: 0;
}

.en {
  margin: 2px 0 0;
  color: var(--color-muted);
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

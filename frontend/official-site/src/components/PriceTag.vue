<script setup lang="ts">
import { computed } from 'vue'
import { formatPrice } from '@/utils/format'

const props = defineProps<{ salePrice: number; listPrice?: number }>()

// 折前價較高才算特價
const onSale = computed(() => props.listPrice !== undefined && props.listPrice > props.salePrice)
</script>

<template>
  <span class="tag">
    <del v-if="onSale" class="old">{{ formatPrice(listPrice!) }}</del>
    <strong class="now" :class="{ sale: onSale }">{{ formatPrice(salePrice) }}</strong>
    <span v-if="onSale" class="flag">(SALE!!)</span>
  </span>
</template>

<style scoped>
.old {
  margin-right: 6px;
  color: var(--color-muted);
  font-weight: 400;
}

.now.sale {
  font-size: 1.15em;
  font-weight: 700;
}

.flag {
  margin-left: 6px;
  color: #d92d20;
  font-weight: 700;
}
</style>

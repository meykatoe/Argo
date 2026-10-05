<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSets, searchCards } from '@/api/card'
import AppPagination from '@/components/AppPagination.vue'
import CardFilterBar from '@/components/CardFilterBar.vue'
import CardTile from '@/components/CardTile.vue'
import type { CardSearchParams, CardSet, CardSummary } from '@/types/card'
import { PAGE_SIZE, parseQuery, toQuery } from '@/utils/query'

const route = useRoute()
const router = useRouter()

const params = ref<CardSearchParams>(parseQuery(route.query))
const cards = ref<CardSummary[]>([])
const sets = ref<CardSet[]>([])
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')

// 只認最後一次請求
let seq = 0

async function load() {
  const current = ++seq
  loading.value = true
  error.value = ''
  try {
    const res = await searchCards({ ...params.value, size: PAGE_SIZE })
    if (current !== seq) return
    cards.value = res.items
    total.value = res.total
    totalPages.value = res.totalPages
  } catch (e) {
    if (current !== seq) return
    error.value = e instanceof Error ? e.message : '載入失敗'
  } finally {
    if (current === seq) loading.value = false
  }
}

// 條件變動回第一頁
function update(patch: CardSearchParams) {
  const isPage = 'page' in patch
  const next = { ...params.value, ...patch, page: isPage ? patch.page : 1 }
  router.push({ query: toQuery(next) })
}

watch(
  () => route.query,
  (q) => {
    params.value = parseQuery(q)
    load()
    window.scrollTo({ top: 0 })
  },
)

onMounted(async () => {
  load()
  try {
    sets.value = await listSets()
  } catch {
    sets.value = []
  }
})
</script>

<template>
  <section>
    <h1>卡片列表</h1>
    <CardFilterBar :model-value="params" :sets="sets" @change="update" />

    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="loading && !cards.length" class="hint">載入中…</p>
    <p v-else-if="!cards.length" class="hint">沒有符合條件的卡片</p>
    <template v-else>
      <p class="hint">共 {{ total }} 張</p>
      <div class="grid" :class="{ loading }">
        <CardTile v-for="c in cards" :key="c.id" :card="c" />
      </div>
      <AppPagination
        :page="params.page ?? 1"
        :total-pages="totalPages"
        @change="update({ page: $event })"
      />
    </template>
  </section>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 16px;
}

.grid.loading {
  opacity: 0.5;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: var(--color-primary);
}
</style>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRouter } from 'vue-router'
import { listSets } from '@/api/card'
import type { CardCategory, CardSet } from '@/types/card'
import { errorText } from '@/utils/error'

const router = useRouter()
const { t, locale } = useI18n()
const sets = ref<CardSet[]>([])
const keyword = ref('')
const error = ref('')

const CATEGORIES: CardCategory[] = ['booster', 'starter', 'promo']

const groups = computed(() =>
  CATEGORIES.map((c) => ({ category: c, sets: sets.value.filter((s) => s.category === c) })),
)

function search() {
  router.push({ path: '/cards', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
}

async function loadSets() {
  error.value = ''
  try {
    sets.value = await listSets()
  } catch (e) {
    error.value = `${t('home.loadError')}：${errorText(e)}`
  }
}

// 切換語言重新取資料
watch(locale, loadSets)
onMounted(loadSets)
</script>

<template>
  <section>
    <div class="hero">
      <h1>{{ t('home.heroTitle') }}</h1>
      <form class="search" @submit.prevent="search">
        <input
          v-model="keyword"
          type="search"
          :placeholder="t('home.searchPlaceholder')"
        />
        <button type="submit">{{ t('home.search') }}</button>
      </form>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-for="g in groups" :key="g.category" class="group">
      <h2>{{ t(`categories.${g.category}`) }}</h2>
      <ul>
        <li v-for="s in g.sets" :key="s.setId">
          <RouterLink :to="{ path: '/cards', query: { setId: s.setId } }">
            <strong>{{ s.setId }}</strong> {{ s.setName }}
          </RouterLink>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.hero {
  text-align: center;
  padding: 40px 0 32px;
}

.search {
  display: flex;
  gap: 8px;
  max-width: 560px;
  margin: 20px auto 0;
}

input,
button {
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font: inherit;
}

input {
  flex: 1;
}

button {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  cursor: pointer;
}

ul {
  list-style: none;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 8px;
}

li a {
  display: block;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
}

li a:hover {
  border-color: var(--color-primary);
}

.error {
  color: var(--color-primary);
}
</style>

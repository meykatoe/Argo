<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { listSets } from '@/api/card'
import type { CardCategory, CardSet } from '@/types/card'

const router = useRouter()
const sets = ref<CardSet[]>([])
const keyword = ref('')
const error = ref('')

const GROUPS: { category: CardCategory; label: string }[] = [
  { category: 'booster', label: '補充包' },
  { category: 'starter', label: '起始牌組' },
  { category: 'promo', label: '促銷卡' },
]

const groups = computed(() =>
  GROUPS.map((g) => ({ ...g, sets: sets.value.filter((s) => s.category === g.category) })),
)

function search() {
  router.push({ path: '/cards', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
}

onMounted(async () => {
  try {
    sets.value = await listSets()
  } catch {
    error.value = '系列載入失敗'
  }
})
</script>

<template>
  <section>
    <div class="hero">
      <h1>尋找屬於你的 ONE PIECE 卡牌</h1>
      <form class="search" @submit.prevent="search">
        <input v-model="keyword" type="search" placeholder="搜尋卡名或編號，例如 Luffy、OP01-001" />
        <button type="submit">搜尋</button>
      </form>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-for="g in groups" :key="g.category" class="group">
      <h2>{{ g.label }}</h2>
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

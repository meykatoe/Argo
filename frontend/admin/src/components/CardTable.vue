<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError, listSetOptions, searchCards } from '@/api/admin'
import type { AdminCard, PageResult, SetOption } from '@/types'
import { errorText } from '@/utils/error'
import CardRow from './CardRow.vue'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const route = useRoute()
const router = useRouter()

// 系列來自網址，可從系列頁直接跳轉
const setId = ref(typeof route.query.setId === 'string' ? route.query.setId : '')
const setOptions = ref<SetOption[]>([])
const keyword = ref('')
const discounted = ref(false)
const page = ref(1)
const data = ref<PageResult<AdminCard> | null>(null)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await searchCards(props.token, {
      keyword: keyword.value.trim(),
      setId: setId.value || undefined,
      discounted: discounted.value || undefined,
      page: page.value,
      size: 20,
    })
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('unauthorized')
    }
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

// 選了系列就寫回網址，重新整理仍保留
function pickSet() {
  router.replace({ path: route.path, query: setId.value ? { setId: setId.value } : {} })
  search()
}

// 從別處跳轉過來時同步系列
watch(
  () => route.query.setId,
  (v) => {
    const next = typeof v === 'string' ? v : ''
    if (next !== setId.value) {
      setId.value = next
      search()
    }
  },
)

// 下拉選單失敗不影響列表
async function loadOptions() {
  try {
    setOptions.value = await listSetOptions()
  } catch {
    setOptions.value = []
  }
}

function go(p: number) {
  page.value = p
  load()
}

// 就地更新該列
function replace(card: AdminCard) {
  if (!data.value) {
    return
  }
  data.value.items = data.value.items.map((c) => (c.id === card.id ? card : c))
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<template>
  <section>
    <form class="bar" @submit.prevent="search">
      <select v-model="setId" aria-label="系列" @change="pickSet">
        <option value="">全部系列</option>
        <option v-for="s in setOptions" :key="s.setId" :value="s.setId">
          {{ s.setId }} {{ s.setName }}{{ s.onSale === 0 ? '（已下架）' : '' }}
        </option>
      </select>
      <input v-model="keyword" placeholder="卡號或卡名" />
      <label><input v-model="discounted" type="checkbox" @change="search" /> 只看有折扣</label>
      <button type="submit" class="primary">搜尋</button>
    </form>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-else-if="loading" class="hint">載入中</p>

    <table v-if="data">
      <thead>
        <tr>
          <th>卡號</th>
          <th>卡名</th>
          <th>參考市價</th>
          <th>折前價</th>
          <th>額外折扣</th>
          <th>售價</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <CardRow
          v-for="c in data.items"
          :key="c.id"
          :card="c"
          :token="token"
          @saved="replace"
          @unauthorized="emit('unauthorized')"
        />
      </tbody>
    </table>
    <p v-if="data && data.items.length === 0" class="hint">沒有符合的卡片</p>

    <div v-if="data && data.totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1 || loading" @click="go(page - 1)">上一頁</button>
      <span>{{ data.page }} / {{ data.totalPages }}</span>
      <button type="button" :disabled="page >= data.totalPages || loading" @click="go(page + 1)">
        下一頁
      </button>
    </div>
  </section>
</template>

<style scoped>
select {
  max-width: 280px;
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font: inherit;
  background: #fff;
}

.bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

th {
  padding: 8px;
  text-align: left;
  border-bottom: 2px solid var(--color-border);
  font-size: 13px;
  color: var(--color-muted);
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: #d92d20;
}
</style>

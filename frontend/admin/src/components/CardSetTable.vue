<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiError, listCardSets } from '@/api/admin'
import type { AdminCardSet } from '@/types'
import { errorText } from '@/utils/error'
import CardSetRow from './CardSetRow.vue'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const sets = ref<AdminCardSet[]>([])
const filter = ref('')
const loaded = ref(false)
const loading = ref(false)
const error = ref('')

const shown = computed(() => {
  const k = filter.value.trim().toLowerCase()
  if (!k) {
    return sets.value
  }
  return sets.value.filter((s) =>
    [s.setId, s.setName, s.setNameEn].some((v) => v.toLowerCase().includes(k)),
  )
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    sets.value = await listCardSets(props.token)
    loaded.value = true
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      emit('unauthorized')
    }
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

// 就地更新該列
function replace(set: AdminCardSet) {
  sets.value = sets.value.map((s) => (s.setId === set.setId ? set : s))
}

onMounted(load)
</script>

<template>
  <section>
    <div class="bar">
      <input v-model="filter" placeholder="系列編號或名稱" aria-label="篩選系列" />
      <span v-if="loaded" class="hint">共 {{ shown.length }} 個系列</span>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-else-if="loading && !loaded" class="hint">載入中</p>

    <table v-if="loaded">
      <thead>
        <tr>
          <th>編號</th>
          <th>系列名稱</th>
          <th>類別</th>
          <th>卡數</th>
          <th>販售狀態</th>
          <th>額外折扣（整個系列）</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <CardSetRow
          v-for="s in shown"
          :key="s.setId"
          :set="s"
          :token="token"
          @saved="replace"
          @unauthorized="emit('unauthorized')"
        />
      </tbody>
    </table>
    <p v-if="loaded && shown.length === 0" class="hint">沒有符合的系列</p>
  </section>
</template>

<style scoped>
.bar {
  display: flex;
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

.hint {
  color: var(--color-muted);
}

.error {
  color: #d92d20;
}
</style>

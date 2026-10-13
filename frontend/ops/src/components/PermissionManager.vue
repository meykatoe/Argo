<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiError, getPermissions, setGrant, setMenuEnabled } from '@/api/ops'
import type { PermissionNode, PermissionOverview } from '@/types'
import { errorText } from '@/utils/error'
import { roleName } from '@shared/utils/role'

const props = defineProps<{ token: string }>()
const emit = defineEmits<{ unauthorized: [] }>()

const data = ref<PermissionOverview | null>(null)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const notice = ref('')

const PORTALS = [
  { portal: 'ADMIN', title: '業務後台' },
  { portal: 'OPS', title: '運維後台' },
] as const

function failed(e: unknown): string {
  if (e instanceof ApiError && e.status === 401) emit('unauthorized')
  return errorText(e)
}

async function load(keepError = false) {
  loading.value = true
  if (!keepError) error.value = ''
  try {
    data.value = await getPermissions(props.token)
  } catch (e) {
    error.value = failed(e)
  } finally {
    loading.value = false
  }
}

const sections = computed(() =>
  PORTALS.map(({ portal, title }) => {
    const nodes = (data.value?.nodes ?? []).filter((n) => n.portal === portal)
    const roles = (data.value?.roles ?? []).filter((r) => (portal === 'OPS') === (r === 'OPS'))
    // 群組在前，底下功能緊跟其後
    const rows: PermissionNode[] = []
    for (const g of nodes.filter((n) => n.parentCode === null)) {
      rows.push(g, ...nodes.filter((n) => n.parentCode === g.code))
    }
    return { portal, title, roles, rows }
  }),
)

async function change(action: () => Promise<PermissionOverview>) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    data.value = await action()
    notice.value = '已儲存，對方重新載入頁面後選單才會更新'
  } catch (e) {
    error.value = failed(e)
    await load(true)
  } finally {
    busy.value = false
  }
}

function toggleGrant(node: PermissionNode, role: string, el: HTMLInputElement) {
  const on = el.checked
  if (!on && !window.confirm(`確定要撤銷「${roleName(role)}」的「${node.title}」嗎？撤銷後立即生效。`)) {
    el.checked = true
    return
  }
  change(() => setGrant(props.token, node.code, role, on))
}

function toggleEnabled(node: PermissionNode, el: HTMLInputElement) {
  const on = el.checked
  if (!on && !window.confirm(`確定要停用「${node.title}」嗎？停用後所有角色立即不能使用。`)) {
    el.checked = true
    return
  }
  change(() => setMenuEnabled(props.token, node.code, on))
}

onMounted(() => load())
</script>

<template>
  <section>
    <p class="hint">勾選代表該角色可以使用此功能，變更立即生效並記入稽核紀錄。群組只能整組啟用或停用。</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="ok" role="status">{{ notice }}</p>
    <p v-if="loading && !data">載入中…</p>

    <template v-if="data">
      <div v-for="s in sections" :key="s.portal" class="block">
        <h2>{{ s.title }}</h2>
        <table :aria-label="s.title + '權限表'">
          <thead>
            <tr>
              <th>功能</th>
              <th class="c">啟用</th>
              <th v-for="r in s.roles" :key="r" class="c">{{ roleName(r) }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="n in s.rows" :key="n.code" :class="{ group: n.group === 1, off: n.enabled === 0 }">
              <td :class="{ leaf: n.group === 0 }">
                {{ n.title }}
                <span v-if="n.enabled === 0" class="tag">已停用</span>
                <span v-if="n.protectedNode" class="tag lock">受保護</span>
                <small class="code">{{ n.code }}</small>
              </td>
              <td class="c">
                <input
                  type="checkbox"
                  :checked="n.enabled === 1"
                  :disabled="busy || n.protectedNode"
                  :aria-label="`啟用 ${n.title}`"
                  @change="toggleEnabled(n, $event.target as HTMLInputElement)"
                />
              </td>
              <td v-for="r in s.roles" :key="r" class="c">
                <input
                  v-if="n.group === 0"
                  type="checkbox"
                  :checked="n.roles.includes(r)"
                  :disabled="busy || (n.protectedNode && r === 'OPS')"
                  :aria-label="`${roleName(r)} 使用 ${n.title}`"
                  @change="toggleGrant(n, r, $event.target as HTMLInputElement)"
                />
                <span v-else class="hint">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>

<style scoped>
.hint {
  color: var(--color-muted);
  font-size: 13px;
}

.error {
  color: #d03b3b;
}

h2 {
  margin: 24px 0 8px;
  font-size: 16px;
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

td {
  padding: 8px;
  border-bottom: 1px solid var(--color-border);
}

.c {
  text-align: center;
  width: 90px;
}

tr.group td {
  background: #f6f6f3;
  font-weight: 600;
}

td.leaf {
  padding-left: 28px;
}

tr.off td {
  color: var(--color-muted);
}

.code {
  margin-left: 8px;
  color: var(--color-muted);
  font-family: ui-monospace, monospace;
  font-weight: 400;
}

.tag {
  margin-left: 8px;
  padding: 1px 6px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 400;
}

.ok {
  color: #0a7a0a;
}

input[type='checkbox'] {
  width: 18px;
  height: 18px;
}
</style>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive } from 'vue'

const props = defineProps<{ ip: string; lockIp: boolean; busy: boolean; error: string }>()
const emit = defineEmits<{ submit: [ip: string, reason: string, hours: number | null]; close: [] }>()

const DURATIONS: { label: string; hours: number | null }[] = [
  { label: '永久', hours: null },
  { label: '1 小時', hours: 1 },
  { label: '24 小時', hours: 24 },
  { label: '7 天', hours: 168 },
  { label: '30 天', hours: 720 },
]

const form = reactive({ ip: props.ip, reason: '', hours: '' as string })
const problem = reactive({ ip: '', reason: '' })

function submit() {
  problem.ip = form.ip.trim() ? '' : '請輸入 IP'
  problem.reason = form.reason.trim() ? '' : '請填寫封鎖原因，之後追查會用到'
  if (problem.ip || problem.reason || props.busy) return
  emit('submit', form.ip.trim(), form.reason.trim(), form.hours === '' ? null : Number(form.hours))
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => window.addEventListener('keydown', onKey))
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div class="backdrop" @click.self="emit('close')">
      <form class="box" role="dialog" aria-modal="true" aria-label="封鎖 IP" novalidate @submit.prevent="submit">
        <h2>封鎖 IP</h2>
        <label>
          IP
          <input v-model="form.ip" :readonly="lockIp" autocomplete="off" placeholder="例如 203.0.113.9" />
          <small v-if="problem.ip" class="err">{{ problem.ip }}</small>
        </label>
        <label>
          原因
          <input v-model="form.reason" maxlength="200" autocomplete="off" placeholder="例如：大量猜密碼" />
          <small v-if="problem.reason" class="err">{{ problem.reason }}</small>
        </label>
        <label>
          封鎖時間
          <select v-model="form.hours">
            <option v-for="d in DURATIONS" :key="d.label" :value="d.hours === null ? '' : String(d.hours)">
              {{ d.label }}
            </option>
          </select>
        </label>
        <p class="warn">封鎖後，這個 IP 無法使用官網與兩個後台，立即生效。</p>
        <p v-if="error" class="err" role="alert">{{ error }}</p>
        <div class="actions">
          <button type="button" @click="emit('close')">取消</button>
          <button type="submit" class="danger" :disabled="busy">確定封鎖</button>
        </div>
      </form>
    </div>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(0 0 0 / 45%);
}

.box {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: min(420px, 92vw);
  padding: 20px;
  border-radius: 10px;
  background: #fff;
}

h2 {
  margin: 0;
  font-size: 18px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: var(--color-muted);
}

input[readonly] {
  background: #f6f6f3;
}

select {
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font: inherit;
}

.warn {
  margin: 0;
  font-size: 13px;
  color: var(--color-muted);
}

.err {
  margin: 0;
  color: #d92d20;
  font-size: 13px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.danger {
  border-color: #d92d20;
  background: #d92d20;
  color: #fff;
}
</style>

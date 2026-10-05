<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{ label: string; error?: string; hint?: string; for?: string }>()

const { t } = useI18n()
</script>

<template>
  <div class="field" :class="{ invalid: error }">
    <label :for="$props.for">{{ label }}</label>
    <slot />
    <small v-if="hint && !error" class="hint">{{ hint }}</small>
    <small v-if="error" class="err" role="alert">{{ t(error) }}</small>
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

label {
  font-size: 14px;
  color: var(--color-muted);
}

.field :deep(input),
.field :deep(select) {
  height: 40px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  font: inherit;
  width: 100%;
}

.invalid :deep(input),
.invalid :deep(select) {
  border-color: var(--color-primary);
}

.hint {
  color: var(--color-muted);
}

.err {
  color: var(--color-primary);
}
</style>

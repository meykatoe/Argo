<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { LOCALES, setLocale, type LocaleCode } from '@/i18n'

const { locale, t } = useI18n()

function change(e: Event) {
  setLocale((e.target as HTMLSelectElement).value as LocaleCode)
}
</script>

<template>
  <select :value="locale" :aria-label="t('lang.label')" class="switcher" @change="change">
    <option v-for="l in LOCALES" :key="l.code" :value="l.code">{{ l.label }}</option>
  </select>
</template>

<style scoped>
.switcher {
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  font: inherit;
}
</style>

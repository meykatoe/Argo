<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <main class="blocked" role="alert">
    <h1>{{ t('blocked.title') }}</h1>
    <p>{{ t('blocked.body') }}</p>
  </main>
</template>

<style scoped>
.blocked {
  max-width: 520px;
  margin: 18vh auto 0;
  padding: 0 24px;
  text-align: center;
}

h1 {
  font-size: 24px;
}

p {
  color: var(--color-muted);
}
</style>

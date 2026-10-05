<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink, RouterView } from 'vue-router'
import logo from '@/assets/logo.svg'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'

const { t } = useI18n()
</script>

<template>
  <header class="header">
    <div class="inner">
      <RouterLink to="/" class="brand" :aria-label="t('site.brand')">
        <img :src="logo" alt="" class="logo" width="40" height="40" />
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/">{{ t('nav.home') }}</RouterLink>
        <RouterLink to="/cards">{{ t('nav.cards') }}</RouterLink>
      </nav>
      <LanguageSwitcher class="lang" />
    </div>
  </header>

  <main class="main">
    <RouterView />
  </main>

  <footer class="footer">
    <div class="inner">{{ t('site.footer') }}</div>
  </footer>
</template>

<style scoped>
.inner {
  max-width: var(--page-width);
  margin: 0 auto;
  padding: 0 16px;
}

.header {
  border-bottom: 1px solid var(--color-border);
  background: #fff;
}

.header .inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 60px;
}

.brand {
  display: flex;
  align-items: center;
}

.logo {
  display: block;
}

.nav {
  display: flex;
  gap: 20px;
}

.lang {
  margin-left: auto;
}

.nav a.router-link-active {
  color: var(--color-primary);
}

.main {
  max-width: var(--page-width);
  margin: 0 auto;
  padding: 24px 16px;
  min-height: calc(100vh - 120px);
}

.footer {
  border-top: 1px solid var(--color-border);
  padding: 16px 0;
  color: var(--color-muted);
  font-size: 14px;
}
</style>

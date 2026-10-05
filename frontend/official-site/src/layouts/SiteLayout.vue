<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink, RouterView } from 'vue-router'
import logo from '@/assets/logo.svg'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import { useCartStore } from '@/stores/cart'

const { t } = useI18n()
const cart = useCartStore()
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
        <RouterLink to="/orders">{{ t('nav.orders') }}</RouterLink>
      </nav>
      <div class="right">
        <RouterLink to="/cart" class="cart" :aria-label="t('cart.title')">
          <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">
            <path
              d="M3 4h2l2.4 10.2a1 1 0 0 0 1 .8h8.7a1 1 0 0 0 1-.8L20 8H6.2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <circle cx="9.5" cy="19" r="1.4" fill="currentColor" />
            <circle cx="16.5" cy="19" r="1.4" fill="currentColor" />
          </svg>
          <span v-if="cart.totalQty > 0" class="count">{{ cart.totalQty }}</span>
        </RouterLink>
        <LanguageSwitcher />
      </div>
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

.right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;
}

.cart {
  position: relative;
  display: flex;
  color: var(--color-text);
}

.cart.router-link-active {
  color: var(--color-primary);
}

.count {
  position: absolute;
  top: -8px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--color-primary);
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
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

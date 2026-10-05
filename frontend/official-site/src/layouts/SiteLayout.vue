<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header class="header">
    <div class="inner">
      <RouterLink to="/" class="brand">Argo</RouterLink>
      <nav class="nav">
        <RouterLink to="/">首頁</RouterLink>
      </nav>
    </div>
  </header>

  <main class="main">
    <RouterView />
  </main>

  <footer class="footer">
    <div class="inner">Argo ONE PIECE 卡牌商店</div>
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
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
}

.nav {
  display: flex;
  gap: 20px;
}

.nav a.router-link-exact-active {
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

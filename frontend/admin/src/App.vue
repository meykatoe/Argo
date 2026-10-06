<script setup lang="ts">
import { ref } from 'vue'
import { logout as logoutApi } from './api/admin'
import CardTable from './components/CardTable.vue'
import LoginForm from './components/LoginForm.vue'
import type { Session } from './types'
import { canManageCards, roleLabel } from './utils/role'

const KEY = 'argo-admin-session'

function saved(): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) {
      return null
    }
    const s = JSON.parse(raw) as Session
    return new Date(s.expiresAt).getTime() > Date.now() ? s : null
  } catch {
    return null
  }
}

// 登入資料只存在分頁階段
const session = ref<Session | null>(saved())

function login(s: Session) {
  session.value = s
  try {
    sessionStorage.setItem(KEY, JSON.stringify(s))
  } catch {
    // 無法儲存也可使用
  }
}

// 本地先登出，伺服器端失敗也無妨
function logout() {
  const token = session.value?.token
  session.value = null
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // 忽略
  }
  if (token) {
    logoutApi(token).catch(() => undefined)
  }
}

// 令牌已失效，只清本地
function expired() {
  session.value = null
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // 忽略
  }
}
</script>

<template>
  <LoginForm v-if="!session" @login="login" />
  <main v-else class="page">
    <header>
      <h1>Argo 後台</h1>
      <div class="who">
        <span>{{ session.username }}（{{ roleLabel(session.role) }}）</span>
        <button type="button" @click="logout">登出</button>
      </div>
    </header>
    <CardTable v-if="canManageCards(session.role)" :token="session.token" @unauthorized="expired" />
    <p v-else class="hint">目前尚無可用功能</p>
  </main>
</template>

<style scoped>
.page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

h1 {
  margin: 0;
  font-size: 20px;
}

.who {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint {
  color: var(--color-muted);
}
</style>

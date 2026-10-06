<script setup lang="ts">
import { ref } from 'vue'
import { logout as logoutApi } from './api/ops'
import AppShell from './components/AppShell.vue'
import LoginForm from './components/LoginForm.vue'
import type { Session } from './types'

const KEY = 'argo-ops-session'

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

// 令牌已失效，只清本地
function expired() {
  session.value = null
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // 忽略
  }
}

// 本地先登出，伺服器端失敗也無妨
function logout() {
  const token = session.value?.token
  expired()
  if (token) {
    logoutApi(token).catch(() => undefined)
  }
}
</script>

<template>
  <LoginForm v-if="!session" @login="login" />
  <AppShell v-else brand="運維後台" :session="session" @logout="logout" @expired="expired" />
</template>

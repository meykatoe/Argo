<script setup lang="ts">
import { ref } from 'vue'
import type { StaffConfig } from '../config'
import type { Session } from '../types'
import AppShell from './AppShell.vue'
import LoginForm from './LoginForm.vue'

const props = defineProps<{ config: StaffConfig }>()

function saved(): Session | null {
  try {
    const raw = sessionStorage.getItem(props.config.storageKey)
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
    sessionStorage.setItem(props.config.storageKey, JSON.stringify(s))
  } catch {
    // 無法儲存也可使用
  }
}

// 令牌已失效，只清本地
function expired() {
  session.value = null
  try {
    sessionStorage.removeItem(props.config.storageKey)
  } catch {
    // 忽略
  }
}

// 本地先登出，伺服器端失敗也無妨
function logout() {
  const token = session.value?.token
  expired()
  if (token) {
    props.config.api.logout(token).catch(() => undefined)
  }
}
</script>

<template>
  <LoginForm v-if="!session" :config="config" @login="login" />
  <AppShell v-else :config="config" :session="session" @logout="logout" @expired="expired" />
</template>

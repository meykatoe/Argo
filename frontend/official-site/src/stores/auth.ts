import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ApiError } from '@/api/http'
import * as api from '@/api/auth'
import { readSession, writeSession, type AuthSession } from '@/utils/authToken'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(readSession())

  const isLoggedIn = computed(
    () => !!session.value && new Date(session.value.expiresAt).getTime() > Date.now(),
  )
  // 顯示用，沒有名字就用 Email
  const displayName = computed(() => session.value?.name || session.value?.username || session.value?.email || '')

  // 先寫入儲存空間，之後的請求才帶得到令牌
  function set(s: AuthSession | null) {
    writeSession(s)
    session.value = s
  }

  async function login(account: string, password: string) {
    set(await api.login(account, password))
  }

  async function register(username: string, email: string, password: string, name: string) {
    set(await api.register(username, email, password, name))
  }

  // 伺服器端失敗也要登出本機
  async function logout() {
    try {
      await api.logout()
    } catch {
      // 忽略
    } finally {
      set(null)
    }
  }

  // 啟動時確認令牌仍有效，失效就清掉
  async function refresh() {
    if (!session.value) return
    try {
      const m = await api.me()
      set({ ...session.value, email: m.email, username: m.username, name: m.name })
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) set(null)
    }
  }

  return { session, isLoggedIn, displayName, login, register, logout, refresh }
})

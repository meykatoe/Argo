<script setup lang="ts">
import { ref } from 'vue'
import CardTable from './components/CardTable.vue'
import LoginForm from './components/LoginForm.vue'

const KEY = 'argo-admin-token'

function saved(): string {
  try {
    return sessionStorage.getItem(KEY) ?? ''
  } catch {
    return ''
  }
}

// 令牌只存在分頁階段
const token = ref(saved())

function login(value: string) {
  token.value = value
  try {
    sessionStorage.setItem(KEY, value)
  } catch {
    // 無法儲存也可使用
  }
}

function logout() {
  token.value = ''
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // 忽略
  }
}
</script>

<template>
  <LoginForm v-if="!token" @login="login" />
  <main v-else class="page">
    <header>
      <h1>Argo 後台 · 額外折扣</h1>
      <button type="button" @click="logout">登出</button>
    </header>
    <CardTable :token="token" @unauthorized="logout" />
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
</style>

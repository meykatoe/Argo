<script setup lang="ts">
import { ref } from 'vue'
import type { StaffConfig } from '../config'
import type { Session } from '../types'

const props = defineProps<{ config: StaffConfig }>()
const emit = defineEmits<{ login: [session: Session] }>()

const username = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')

async function submit() {
  if (!username.value.trim() || !password.value || busy.value) {
    return
  }
  busy.value = true
  error.value = ''
  try {
    emit('login', await props.config.api.login(username.value.trim(), password.value))
    // 登入後不留密碼
    password.value = ''
  } catch (e) {
    error.value = props.config.errors.lockedMessage(e) ?? props.config.errors.errorText(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="login" @submit.prevent="submit">
    <h1>{{ config.title }}</h1>
    <input v-model="username" placeholder="帳號" autocomplete="username" />
    <input v-model="password" type="password" placeholder="密碼" autocomplete="current-password" />
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <button type="submit" class="primary" :disabled="busy">登入</button>
  </form>
</template>

<style scoped>
.login {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 320px;
  margin: 15vh auto 0;
  padding: 0 16px;
}

.error {
  margin: 0;
  color: #d92d20;
}
</style>

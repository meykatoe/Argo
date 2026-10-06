<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ login: [token: string] }>()
const token = ref('')

function submit() {
  if (token.value.trim()) {
    emit('login', token.value.trim())
  }
}
</script>

<template>
  <form class="login" @submit.prevent="submit">
    <h1>Argo 後台</h1>
    <input v-model="token" type="password" placeholder="後台令牌" autocomplete="off" />
    <button type="submit" class="primary">登入</button>
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
</style>

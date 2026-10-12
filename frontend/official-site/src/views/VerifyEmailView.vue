<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { errorText } from '@/utils/error'

const { t } = useI18n()
const route = useRoute()
const auth = useAuthStore()

const state = ref<'loading' | 'ok' | 'fail'>('loading')
const error = ref('')

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (!token) {
    error.value = t('error.TOKEN_INVALID')
    state.value = 'fail'
    return
  }
  try {
    await auth.verifyLink(token)
    state.value = 'ok'
  } catch (e) {
    error.value = errorText(e)
    state.value = 'fail'
  }
})
</script>

<template>
  <section class="verify">
    <h1>{{ t('auth.verifyTitle') }}</h1>
    <p v-if="state === 'loading'">{{ t('auth.verifying') }}</p>
    <p v-else-if="state === 'ok'" class="ok" role="status">{{ t('auth.verifyDone') }}</p>
    <template v-else>
      <p class="error" role="alert">{{ error }}</p>
      <p class="hint">{{ t('auth.verifyFailHint') }}</p>
    </template>
    <p>
      <RouterLink :to="{ name: auth.isLoggedIn ? 'account' : 'login' }">
        {{ auth.isLoggedIn ? t('account.title') : t('auth.goLogin') }}
      </RouterLink>
    </p>
  </section>
</template>

<style scoped>
.verify {
  max-width: 420px;
}

.error {
  color: #d92d20;
}

.ok {
  color: #067647;
}

.hint {
  color: var(--color-muted);
  font-size: 14px;
}

a {
  color: var(--color-primary);
}
</style>

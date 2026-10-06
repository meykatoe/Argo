<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormField from '@/components/FormField.vue'
import { useAuthStore } from '@/stores/auth'
import { errorText, lockedMinutes } from '@/utils/error'
import { safeRedirect } from '@/utils/redirect'
import { isEmail } from '@/utils/validators'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ email: '', password: '' })
const errors = reactive<Record<string, string>>({})
const busy = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  errors.email = isEmail(form.email) ? '' : 'validation.email'
  errors.password = form.password ? '' : 'validation.required'
  if (errors.email || errors.password || busy.value) return
  busy.value = true
  try {
    await auth.login(form.email.trim(), form.password)
    form.password = ''
    router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    const wait = lockedMinutes(e)
    error.value = wait ? t('auth.lockedFor', { n: wait }) : errorText(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>{{ t('auth.loginTitle') }}</h1>
    <form novalidate @submit.prevent="submit">
      <FormField :label="t('auth.email')" :error="errors.email" for="lg-email">
        <input id="lg-email" v-model="form.email" type="email" autocomplete="email" />
      </FormField>
      <FormField :label="t('auth.password')" :error="errors.password" for="lg-pw">
        <input id="lg-pw" v-model="form.password" type="password" autocomplete="current-password" />
      </FormField>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" class="primary" :disabled="busy">{{ t('auth.loginSubmit') }}</button>
    </form>
    <p class="alt">
      {{ t('auth.noAccount') }}
      <RouterLink :to="{ name: 'register', query: route.query }">{{ t('auth.goRegister') }}</RouterLink>
    </p>
    <p class="hint">{{ t('auth.guestHint') }}</p>
  </section>
</template>

<style scoped>
.auth {
  max-width: 420px;
}

form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.primary {
  height: 42px;
  border: 0;
  border-radius: 8px;
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #d92d20;
}

.alt a {
  color: var(--color-primary);
}

.hint {
  color: var(--color-muted);
  font-size: 14px;
}
</style>

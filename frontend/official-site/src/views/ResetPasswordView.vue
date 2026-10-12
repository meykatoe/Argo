<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import { resetPassword } from '@/api/auth'
import FormField from '@/components/FormField.vue'
import { useAuthStore } from '@/stores/auth'
import { errorText } from '@/utils/error'
import { isPasswordLongEnough, isPasswordMixed, isPasswordTooLong } from '@/utils/validators'

const { t } = useI18n()
const route = useRoute()
const auth = useAuthStore()

const token = typeof route.query.token === 'string' ? route.query.token : ''
const form = reactive({ password: '', confirm: '' })
const errors = reactive<Record<string, string>>({})
const error = ref('')
const busy = ref(false)
const done = ref(false)

function validate(): boolean {
  errors.password = !isPasswordLongEnough(form.password)
    ? 'validation.passwordShort'
    : isPasswordTooLong(form.password)
      ? 'validation.passwordLong'
      : !isPasswordMixed(form.password)
        ? 'validation.passwordWeak'
        : ''
  errors.confirm = form.confirm === form.password ? '' : 'validation.passwordMismatch'
  return Object.values(errors).every((e) => !e)
}

async function submit() {
  error.value = ''
  if (!validate() || busy.value) return
  busy.value = true
  try {
    await resetPassword(token, form.password)
    form.password = ''
    form.confirm = ''
    // 所有登入都已作廢，清掉本機狀態
    await auth.logout()
    done.value = true
  } catch (e) {
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>{{ t('auth.resetTitle') }}</h1>
    <p v-if="done" class="ok" role="status">{{ t('auth.resetDone') }}</p>
    <p v-else-if="!token" class="error" role="alert">{{ t('error.TOKEN_INVALID') }}</p>
    <form v-else novalidate @submit.prevent="submit">
      <FormField :label="t('auth.newPassword')" :error="errors.password" :hint="t('auth.passwordHint')" for="rp-pw">
        <input id="rp-pw" v-model="form.password" type="password" autocomplete="new-password" />
      </FormField>
      <FormField :label="t('auth.confirm')" :error="errors.confirm" for="rp-confirm">
        <input id="rp-confirm" v-model="form.confirm" type="password" autocomplete="new-password" />
      </FormField>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" class="primary" :disabled="busy">{{ t('auth.resetSubmit') }}</button>
    </form>
    <p class="alt">
      <RouterLink v-if="done" :to="{ name: 'login' }">{{ t('auth.goLogin') }}</RouterLink>
      <RouterLink v-else :to="{ name: 'forgotPassword' }">{{ t('auth.forgotLink') }}</RouterLink>
    </p>
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

.ok {
  color: #067647;
}

.alt a {
  color: var(--color-primary);
}
</style>

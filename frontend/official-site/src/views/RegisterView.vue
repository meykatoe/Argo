<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormField from '@/components/FormField.vue'
import { useAuthStore } from '@/stores/auth'
import { errorText } from '@/utils/error'
import { safeRedirect } from '@/utils/redirect'
import { isEmail, isPasswordLongEnough, isPasswordTooLong } from '@/utils/validators'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ email: '', name: '', password: '', confirm: '' })
const errors = reactive<Record<string, string>>({})
const busy = ref(false)
const error = ref('')

function validate(): boolean {
  errors.email = isEmail(form.email) ? '' : 'validation.email'
  errors.password = !isPasswordLongEnough(form.password)
    ? 'validation.passwordShort'
    : isPasswordTooLong(form.password)
      ? 'validation.passwordLong'
      : ''
  errors.confirm = form.confirm === form.password ? '' : 'validation.passwordMismatch'
  return Object.values(errors).every((e) => !e)
}

async function submit() {
  error.value = ''
  if (!validate() || busy.value) return
  busy.value = true
  try {
    await auth.register(form.email.trim(), form.password, form.name.trim())
    form.password = ''
    form.confirm = ''
    router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>{{ t('auth.registerTitle') }}</h1>
    <form novalidate @submit.prevent="submit">
      <FormField :label="t('auth.email')" :error="errors.email" for="rg-email">
        <input id="rg-email" v-model="form.email" type="email" autocomplete="email" />
      </FormField>
      <FormField :label="t('auth.nameOptional')" for="rg-name">
        <input id="rg-name" v-model="form.name" maxlength="100" autocomplete="name" />
      </FormField>
      <FormField :label="t('auth.password')" :hint="t('auth.passwordHint')" :error="errors.password" for="rg-pw">
        <input id="rg-pw" v-model="form.password" type="password" autocomplete="new-password" />
      </FormField>
      <FormField :label="t('auth.confirm')" :error="errors.confirm" for="rg-confirm">
        <input id="rg-confirm" v-model="form.confirm" type="password" autocomplete="new-password" />
      </FormField>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" class="primary" :disabled="busy">{{ t('auth.registerSubmit') }}</button>
    </form>
    <p class="alt">
      {{ t('auth.haveAccount') }}
      <RouterLink :to="{ name: 'login', query: route.query }">{{ t('auth.goLogin') }}</RouterLink>
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

.alt a {
  color: var(--color-primary);
}
</style>

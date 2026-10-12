<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import { forgotPassword } from '@/api/auth'
import FormField from '@/components/FormField.vue'
import { errorText } from '@/utils/error'
import { isEmail } from '@/utils/validators'

const { t } = useI18n()

const email = ref('')
const fieldError = ref('')
const error = ref('')
const busy = ref(false)
const sent = ref(false)

async function submit() {
  error.value = ''
  fieldError.value = isEmail(email.value) ? '' : 'validation.email'
  if (fieldError.value || busy.value) return
  busy.value = true
  try {
    await forgotPassword(email.value.trim())
    sent.value = true
  } catch (e) {
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>{{ t('auth.forgotTitle') }}</h1>
    <p v-if="sent" class="ok" role="status">{{ t('auth.forgotSent') }}</p>
    <form v-else novalidate @submit.prevent="submit">
      <p class="hint">{{ t('auth.forgotHint') }}</p>
      <FormField :label="t('auth.email')" :error="fieldError" for="fp-email">
        <input id="fp-email" v-model="email" type="email" autocomplete="email" />
      </FormField>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" class="primary" :disabled="busy">{{ t('auth.forgotSubmit') }}</button>
    </form>
    <p class="alt">
      <RouterLink :to="{ name: 'login' }">{{ t('auth.goLogin') }}</RouterLink>
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

.hint {
  margin: 0;
  color: var(--color-muted);
  font-size: 14px;
}
</style>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormField from '@/components/FormField.vue'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/http'
import { errorText } from '@/utils/error'
import { safeRedirect } from '@/utils/redirect'
import { resendVerification, usernameAvailable } from '@/api/auth'
import { isEmail, isPasswordLongEnough, isPasswordMixed, isPasswordTooLong, isUsername } from '@/utils/validators'

const RESEND_SECONDS = 60

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', email: '', name: '', password: '', confirm: '' })
const errors = reactive<Record<string, string>>({})
const busy = ref(false)
const error = ref('')

// 送出後等使用者驗證信箱
const step = ref<'form' | 'verify'>('form')
const sentTo = ref('')
const code = ref('')
const codeError = ref('')
const notice = ref('')
const cooldown = ref(0)
let timer: ReturnType<typeof setInterval> | undefined

function startCooldown(seconds: number) {
  cooldown.value = seconds
  clearInterval(timer)
  timer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) clearInterval(timer)
  }, 1000)
}

onBeforeUnmount(() => clearInterval(timer))

// 離開帳號欄位時檢查是否已被使用，檢查失敗不擋註冊，送出時後端仍會確認
async function checkUsername() {
  const name = form.username.trim()
  if (!isUsername(name)) {
    errors.username = name ? 'validation.usernameInvalid' : ''
    return
  }
  try {
    const r = await usernameAvailable(name)
    // 回應回來時欄位若已改過就忽略
    if (form.username.trim() === name) {
      errors.username = r.available === 1 ? '' : 'validation.usernameTaken'
    }
  } catch {
    errors.username = ''
  }
}

function validate(): boolean {
  if (!isUsername(form.username)) {
    errors.username = 'validation.usernameInvalid'
  } else if (errors.username !== 'validation.usernameTaken') {
    errors.username = ''
  }
  errors.email = isEmail(form.email) ? '' : 'validation.email'
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
    await auth.register(form.username.trim(), form.email.trim(), form.password, form.name.trim())
    sentTo.value = form.email.trim()
    form.password = ''
    form.confirm = ''
    step.value = 'verify'
    startCooldown(RESEND_SECONDS)
  } catch (e) {
    // 帳號被搶先註冊時標在帳號欄位上
    if (e instanceof ApiError && e.code === 'USERNAME_TAKEN') {
      errors.username = 'validation.usernameTaken'
    } else {
      error.value = errorText(e)
    }
  } finally {
    busy.value = false
  }
}

async function submitCode() {
  codeError.value = ''
  notice.value = ''
  if (!/^\d{6}$/.test(code.value.trim())) {
    codeError.value = 'validation.codeFormat'
    return
  }
  if (busy.value) return
  busy.value = true
  try {
    await auth.verifyCode(sentTo.value, code.value.trim())
    router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}

async function resend() {
  if (cooldown.value > 0) return
  error.value = ''
  notice.value = ''
  try {
    await resendVerification(sentTo.value)
    notice.value = t('auth.resent')
    code.value = ''
    startCooldown(RESEND_SECONDS)
  } catch (e) {
    if (e instanceof ApiError && e.code === 'RESEND_TOO_SOON') {
      startCooldown(Number(e.details.retryAfterSeconds) || RESEND_SECONDS)
    } else {
      error.value = errorText(e)
    }
  }
}

// 改填別的信箱或資料
function back() {
  step.value = 'form'
  error.value = ''
  notice.value = ''
  code.value = ''
  clearInterval(timer)
  cooldown.value = 0
}
</script>

<template>
  <section v-if="step === 'verify'" class="auth">
    <h1>{{ t('auth.checkMailTitle') }}</h1>
    <p>{{ t('auth.checkMailBody', { email: sentTo }) }}</p>
    <form novalidate @submit.prevent="submitCode">
      <FormField :label="t('auth.codeLabel')" :hint="t('auth.codeHint')" :error="codeError" for="rg-code">
        <input
          id="rg-code"
          v-model="code"
          maxlength="6"
          inputmode="numeric"
          autocomplete="one-time-code"
          @input="codeError = ''"
        />
      </FormField>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <button type="submit" class="primary" :disabled="busy">{{ t('auth.codeSubmit') }}</button>
    </form>
    <p class="alt">
      <button type="button" class="link" :disabled="cooldown > 0" @click="resend">
        {{ cooldown > 0 ? t('auth.resendIn', { n: cooldown }) : t('auth.resend') }}
      </button>
      ·
      <button type="button" class="link" @click="back">{{ t('auth.changeEmail') }}</button>
    </p>
  </section>

  <section v-else class="auth">
    <h1>{{ t('auth.registerTitle') }}</h1>
    <form novalidate @submit.prevent="submit">
      <FormField :label="t('auth.username')" :hint="t('auth.usernameHint')" :error="errors.username" for="rg-username">
        <input
          id="rg-username"
          v-model="form.username"
          maxlength="30"
          autocomplete="username"
          @blur="checkUsername"
          @input="errors.username = ''"
        />
      </FormField>
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

.notice {
  margin: 0;
  color: #067647;
}

.link {
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-primary);
  font: inherit;
  cursor: pointer;
}

.link:disabled {
  color: var(--color-muted);
  cursor: not-allowed;
}
</style>

<script setup lang="ts">
import { reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import FormField from '@/components/FormField.vue'
import { saveOrderEmail } from '@/utils/orderAccess'
import { isEmail } from '@/utils/validators'

const { t } = useI18n()
const router = useRouter()
const form = reactive({ orderNo: '', email: '' })
const errors = reactive<Record<string, string>>({})

function submit() {
  errors.orderNo = form.orderNo.trim() ? '' : 'validation.required'
  errors.email = isEmail(form.email) ? '' : 'validation.email'
  if (errors.orderNo || errors.email) return
  // 編號一律大寫
  const orderNo = form.orderNo.trim().toUpperCase()
  saveOrderEmail(orderNo, form.email.trim())
  router.push({ name: 'orderDetail', params: { orderNo } })
}
</script>

<template>
  <section class="lookup">
    <h1>{{ t('lookup.title') }}</h1>
    <form novalidate @submit.prevent="submit">
      <FormField :label="t('lookup.orderNo')" :error="errors.orderNo" for="lk-no">
        <input id="lk-no" v-model="form.orderNo" placeholder="AR261005-ABC234" autocomplete="off" />
      </FormField>
      <FormField :label="t('lookup.email')" :error="errors.email" for="lk-email">
        <input id="lk-email" v-model="form.email" type="email" autocomplete="email" />
      </FormField>
      <button type="submit" class="primary">{{ t('lookup.submit') }}</button>
    </form>
  </section>
</template>

<style scoped>
.lookup {
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
</style>

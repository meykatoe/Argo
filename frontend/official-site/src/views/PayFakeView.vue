<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { fakePay } from '@/api/order'
import { errorText } from '@/utils/error'
import { getOrderEmail } from '@/utils/orderAccess'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const orderNo = computed(() => String(route.query.order ?? ''))
const trade = computed(() => String(route.query.trade ?? ''))
const busy = ref(false)
const error = ref('')

async function finish(success: 0 | 1) {
  busy.value = true
  error.value = ''
  try {
    await fakePay(orderNo.value, getOrderEmail(orderNo.value), trade.value, success)
    await router.push({ name: 'payResult', query: { order: orderNo.value } })
  } catch (e) {
    error.value = errorText(e)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="fake">
    <h1>{{ t('payFake.title') }}</h1>
    <p class="hint">{{ t('payFake.hint') }}</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <div class="actions">
      <button type="button" class="primary" :disabled="busy" @click="finish(1)">
        {{ t('payFake.success') }}
      </button>
      <button type="button" class="ghost" :disabled="busy" @click="finish(0)">
        {{ t('payFake.fail') }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.fake {
  max-width: 420px;
}

.hint {
  color: var(--color-muted);
}

.error {
  color: var(--color-primary);
}

.actions {
  display: flex;
  gap: 12px;
}

.primary,
.ghost {
  height: 44px;
  padding: 0 20px;
  border-radius: 8px;
  font: inherit;
  cursor: pointer;
}

.primary {
  border: 0;
  background: var(--color-primary);
  color: #fff;
}

.ghost {
  border: 1px solid var(--color-border);
  background: #fff;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>

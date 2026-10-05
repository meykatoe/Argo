import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import { getOrderEmail } from '@/utils/orderAccess'
import OrderLookupView from '../OrderLookupView.vue'

async function setup() {
  sessionStorage.clear()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/orders', component: OrderLookupView },
      { path: '/orders/:orderNo', name: 'orderDetail', component: { template: '<i/>' } },
    ],
  })
  await router.push('/orders')
  const w = mount(OrderLookupView, { global: { plugins: [router, i18n] } })
  return { w, router }
}

describe('OrderLookupView', () => {
  beforeEach(() => sessionStorage.clear())

  it('欄位不完整不會前往', async () => {
    const { w, router } = await setup()
    await w.find('form').trigger('submit')
    expect(router.currentRoute.value.path).toBe('/orders')
    expect(w.text()).toContain('此欄位必填')
    expect(w.text()).toContain('Email 格式不正確')
  })

  it('前往訂單頁並記住信箱，編號轉大寫', async () => {
    const { w, router } = await setup()
    await w.find('#lk-no').setValue(' ar261005-abc234 ')
    await w.find('#lk-email').setValue('ming@example.com')
    await w.find('form').trigger('submit')
    await new Promise((r) => setTimeout(r, 0))
    expect(router.currentRoute.value.path).toBe('/orders/AR261005-ABC234')
    expect(getOrderEmail('AR261005-ABC234')).toBe('ming@example.com')
  })
})

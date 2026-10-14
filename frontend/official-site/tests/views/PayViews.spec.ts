import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'
import type { Order } from '@/types/order'
import { saveOrderEmail } from '@/utils/orderAccess'
import PayFakeView from '@/views/PayFakeView.vue'
import PayResultView from '@/views/PayResultView.vue'

vi.mock('@/api/order', () => ({ getOrder: vi.fn(), fakePay: vi.fn() }))
import { fakePay, getOrder } from '@/api/order'

const NO = 'AR261005-ABC234'

function order(extra: Partial<Order> = {}): Order {
  return { orderNo: NO, status: 'PENDING_PAYMENT', payment: null, ...extra } as Order
}

async function mountAt(component: object, path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/pay/result', name: 'payResult', component },
      { path: '/pay/fake', name: 'payFake', component },
      { path: '/orders/:orderNo', name: 'orderDetail', component: { template: '<i/>' } },
    ],
  })
  await router.push(path)
  const w = mount(component, { global: { plugins: [router, i18n] } })
  await flushPromises()
  return { w, router }
}

describe('PayResultView', () => {
  beforeEach(() => {
    sessionStorage.clear()
    saveOrderEmail(NO, 'ming@example.com')
    vi.mocked(getOrder).mockReset()
  })
  afterEach(() => vi.useRealTimers())

  it('沒有訂單編號時提示', async () => {
    const { w } = await mountAt(PayResultView, '/pay/result')
    expect(w.text()).toContain('找不到訂單資訊')
    expect(getOrder).not.toHaveBeenCalled()
  })

  it('已付款顯示成功', async () => {
    vi.mocked(getOrder).mockResolvedValue(order({ status: 'PAID' }))
    const { w } = await mountAt(PayResultView, `/pay/result?order=${NO}`)
    expect(w.text()).toContain('付款成功')
    expect(getOrder).toHaveBeenCalledTimes(1)
  })

  it('付款失敗顯示失敗', async () => {
    vi.mocked(getOrder).mockResolvedValue(
      order({ payment: { status: 'FAILED', cardLast4: null, failureCode: 'X', createdAt: '' } }),
    )
    const { w } = await mountAt(PayResultView, `/pay/result?order=${NO}`)
    expect(w.text()).toContain('付款沒有成功')
  })

  it('尚未收到結果會再查，之後成功', async () => {
    vi.useFakeTimers()
    vi.mocked(getOrder)
      .mockResolvedValueOnce(order())
      .mockResolvedValueOnce(order({ status: 'PAID' }))
    const { w } = await mountAt(PayResultView, `/pay/result?order=${NO}`)
    expect(w.text()).toContain('正在確認')
    await vi.advanceTimersByTimeAsync(2100)
    await flushPromises()
    expect(w.text()).toContain('付款成功')
  })
})

describe('PayFakeView', () => {
  beforeEach(() => {
    sessionStorage.clear()
    saveOrderEmail(NO, 'ming@example.com')
    vi.mocked(fakePay).mockReset()
  })

  it('成功與失敗各送出對應的旗標', async () => {
    vi.mocked(fakePay).mockResolvedValue(order())
    const { w, router } = await mountAt(PayFakeView, `/pay/fake?order=${NO}&trade=T1`)
    const buttons = w.findAll('button')
    await buttons[0]!.trigger('click')
    await flushPromises()
    expect(fakePay).toHaveBeenCalledWith(NO, 'ming@example.com', 'T1', 1)
    expect(router.currentRoute.value.fullPath).toBe(`/pay/result?order=${NO}`)
    await router.push(`/pay/fake?order=${NO}&trade=T2`)
    await buttons[1]!.trigger('click')
    await flushPromises()
    expect(fakePay).toHaveBeenLastCalledWith(NO, 'ming@example.com', 'T2', 0)
  })

  it('非模擬模式顯示錯誤', async () => {
    vi.mocked(fakePay).mockRejectedValue(new ApiError(404, 'PAYMENT_UNAVAILABLE'))
    const { w } = await mountAt(PayFakeView, `/pay/fake?order=${NO}&trade=T1`)
    await w.findAll('button')[0]!.trigger('click')
    await flushPromises()
    expect(w.find('[role="alert"]').exists()).toBe(true)
  })
})

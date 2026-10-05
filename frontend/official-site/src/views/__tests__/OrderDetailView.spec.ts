import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'
import type { Order } from '@/types/order'
import { getOrderEmail, saveOrderEmail } from '@/utils/orderAccess'
import OrderDetailView from '../OrderDetailView.vue'

vi.mock('@/api/order', () => ({ getOrder: vi.fn(), payOrder: vi.fn(), cancelOrder: vi.fn() }))
import { cancelOrder, getOrder, payOrder } from '@/api/order'

function order(extra: Partial<Order> = {}): Order {
  return {
    orderNo: 'AR261005-ABC234',
    status: 'PENDING_PAYMENT',
    currency: 'USD',
    subtotal: 3,
    shippingFee: 0,
    total: 3,
    createdAt: new Date().toISOString(),
    expiresAt: new Date(Date.now() + 20 * 60_000).toISOString(),
    paidAt: null,
    cancelReason: null,
    customerName: '王小明',
    customerEmail: 'ming@example.com',
    recipientName: '王小明',
    recipientPhone: '0912345678',
    postalCode: '100',
    city: '台北市',
    address: '中正區',
    items: [
      {
        cardId: 1,
        cardSetId: 'OP01-001',
        cardName: '羅羅亞・索隆',
        cardNameEn: 'Roronoa Zoro',
        imageUrl: null,
        unitPrice: 1.5,
        quantity: 2,
        subtotal: 3,
      },
    ],
    payment: null,
    ...extra,
  }
}

type Wrapper = ReturnType<typeof mount>
const mounted: Wrapper[] = []

async function mountOrder(withEmail = true) {
  sessionStorage.clear()
  if (withEmail) saveOrderEmail('AR261005-ABC234', 'ming@example.com')
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/orders', component: { template: '<i/>' } },
      { path: '/orders/:orderNo', name: 'orderDetail', component: OrderDetailView },
    ],
  })
  await router.push('/orders/AR261005-ABC234')
  const w = mount(OrderDetailView, { global: { plugins: [router, i18n] } })
  mounted.push(w)
  await flushPromises()
  return w
}

async function fillCard(w: Wrapper, number = '4242424242424242') {
  await w.find('#p-number').setValue(number)
  await w.find('#p-month').setValue('12')
  await w.find('#p-year').setValue('2099')
  await w.find('#p-cvc').setValue('123')
  await w.find('#p-holder').setValue('WANG')
}

describe('OrderDetailView', () => {
  beforeEach(() => {
    vi.mocked(getOrder).mockReset()
    vi.mocked(payOrder).mockReset()
    vi.mocked(cancelOrder).mockReset()
  })
  afterEach(() => {
    mounted.splice(0).forEach((w) => w.unmount())
    vi.unstubAllGlobals()
  })

  it('沒有信箱時先要求輸入', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    const w = await mountOrder(false)
    expect(getOrder).not.toHaveBeenCalled()
    await w.find('#o-email').setValue('bad')
    await w.find('form').trigger('submit')
    expect(getOrder).not.toHaveBeenCalled()
    await w.find('#o-email').setValue('ming@example.com')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(getOrder).toHaveBeenCalledWith('AR261005-ABC234', 'ming@example.com')
    expect(getOrderEmail('AR261005-ABC234')).toBe('ming@example.com')
    expect(w.text()).toContain('AR261005-ABC234')
  })

  it('待付款顯示明細、倒數與付款表單', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    const w = await mountOrder()
    expect(w.text()).toContain('待付款')
    expect(w.text()).toContain('羅羅亞・索隆')
    expect(w.text()).toContain('US$ 3.00')
    expect(w.text()).toContain('免運')
    expect(w.text()).toMatch(/請於 \d{2}:\d{2} 內完成付款/)
    expect(w.find('#p-number').exists()).toBe(true)
  })

  it('卡片資料不正確不會送出', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    const w = await mountOrder()
    await fillCard(w, '4242424242424241')
    await w.find('form.pay').trigger('submit')
    expect(payOrder).not.toHaveBeenCalled()
    expect(w.text()).toContain('卡號不正確')
  })

  it('已過期的卡不會送出', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    const w = await mountOrder()
    await fillCard(w)
    await w.find('#p-year').setValue('2020')
    await w.find('form.pay').trigger('submit')
    expect(payOrder).not.toHaveBeenCalled()
    expect(w.text()).toContain('卡片已過期')
  })

  it('付款成功顯示成功畫面並隱藏表單', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    vi.mocked(payOrder).mockResolvedValue(
      order({ status: 'PAID', paidAt: new Date().toISOString(), payment: { status: 'SUCCEEDED', cardLast4: '4242', failureCode: null, createdAt: '' } }),
    )
    const w = await mountOrder()
    await w.find('#p-number').setValue('4242 4242 4242 4242')
    await w.find('#p-month').setValue('12')
    await w.find('#p-year').setValue('2099')
    await w.find('#p-cvc').setValue('123')
    await w.find('#p-holder').setValue('WANG')
    await w.find('form.pay').trigger('submit')
    await flushPromises()

    const [no, body] = vi.mocked(payOrder).mock.calls[0]!
    expect(no).toBe('AR261005-ABC234')
    expect(body.card).toEqual({ number: '4242424242424242', expMonth: 12, expYear: 2099, cvc: '123', holderName: 'WANG' })
    expect(body.email).toBe('ming@example.com')
    expect(w.text()).toContain('付款成功')
    expect(w.text()).toContain('末四碼 4242')
    expect(w.find('#p-number').exists()).toBe(false)
  })

  it('卡片被拒絕時顯示原因並保留表單', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    vi.mocked(payOrder).mockRejectedValue(new ApiError(402, 'CARD_DECLINED'))
    const w = await mountOrder()
    await fillCard(w, '4000000000000002')
    await w.find('form.pay').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('信用卡被拒絕')
    expect(w.find('#p-number').exists()).toBe(true)
    expect(w.find('form.pay button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  it('付款逾期會重新載入訂單', async () => {
    vi.mocked(getOrder)
      .mockResolvedValueOnce(order())
      .mockResolvedValueOnce(order({ status: 'CANCELLED', cancelReason: 'EXPIRED' }))
    vi.mocked(payOrder).mockRejectedValue(new ApiError(410, 'ORDER_EXPIRED'))
    const w = await mountOrder()
    await fillCard(w)
    await w.find('form.pay').trigger('submit')
    await flushPromises()
    expect(getOrder).toHaveBeenCalledTimes(2)
    expect(w.text()).toContain('付款逾時，訂單已自動取消')
    expect(w.find('#p-number').exists()).toBe(false)
  })

  it('可以取消訂單', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    vi.mocked(cancelOrder).mockResolvedValue(order({ status: 'CANCELLED', cancelReason: 'CUSTOMER' }))
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))
    const w = await mountOrder()
    await w.find('button.ghost').trigger('click')
    await flushPromises()
    expect(cancelOrder).toHaveBeenCalledWith('AR261005-ABC234', 'ming@example.com')
    expect(w.text()).toContain('訂單已取消')
  })

  it('不確認就不取消', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(false))
    const w = await mountOrder()
    await w.find('button.ghost').trigger('click')
    expect(cancelOrder).not.toHaveBeenCalled()
  })

  it('信箱不符時回到輸入畫面', async () => {
    vi.mocked(getOrder).mockRejectedValue(new ApiError(404, 'ORDER_NOT_FOUND'))
    const w = await mountOrder()
    expect(w.text()).toContain('找不到訂單')
    expect(w.find('#o-email').exists()).toBe(true)
    expect(getOrderEmail('AR261005-ABC234')).toBe('')
  })
})

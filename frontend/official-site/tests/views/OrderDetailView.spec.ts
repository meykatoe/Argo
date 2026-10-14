import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'
import type { Order } from '@/types/order'
import { getOrderEmail, saveOrderEmail } from '@/utils/orderAccess'
import OrderDetailView from '@/views/OrderDetailView.vue'

vi.mock('@/api/order', () => ({ getOrder: vi.fn(), payOrder: vi.fn(), cancelOrder: vi.fn() }))
vi.mock('@/utils/ecpayForm', () => ({ submitForm: vi.fn() }))
import { submitForm } from '@/utils/ecpayForm'
import { cancelOrder, getOrder, payOrder } from '@/api/order'

function order(extra: Partial<Order> = {}): Order {
  return {
    orderNo: 'AR261005-ABC234',
    status: 'PENDING_PAYMENT',
    currency: 'USD',
    subtotal: 3,
    shippingFee: 0,
    total: 3,
    totalTwd: 96,
    fxRate: 32,
    createdAt: new Date().toISOString(),
    expiresAt: new Date(Date.now() + 20 * 60_000).toISOString(),
    paidAt: null,
    shippedAt: null,
    trackingNo: null,
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
      { path: '/pay/fake', name: 'payFake', component: { template: '<i/>' } },
      { path: '/orders/:orderNo', name: 'orderDetail', component: OrderDetailView },
    ],
  })
  await router.push('/orders/AR261005-ABC234')
  const w = mount(OrderDetailView, { global: { plugins: [router, i18n] } })
  mounted.push(w)
  await flushPromises()
  return w
}

describe('OrderDetailView', () => {
  beforeEach(() => {
    vi.mocked(getOrder).mockReset()
    vi.mocked(payOrder).mockReset()
    vi.mocked(cancelOrder).mockReset()
    vi.mocked(submitForm).mockReset()
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

  it('待付款顯示明細、倒數與新台幣金額', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    const w = await mountOrder()
    expect(w.text()).toContain('待付款')
    expect(w.text()).toContain('羅羅亞・索隆')
    expect(w.text()).toContain('US$ 3.00')
    expect(w.text()).toContain('免運')
    expect(w.text()).toMatch(/請於 \d{2}:\d{2} 內完成付款/)
    expect(w.text()).toContain('NT$ 96')
    expect(w.text()).toContain('1 USD = 32 TWD')
    expect(w.find('input').exists()).toBe(false)
  })

  it('綠界模式送出表單', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    vi.mocked(payOrder).mockResolvedValue({
      mode: 'ECPAY',
      tradeNo: 'AR26100501',
      action: 'https://pay.test/aio',
      fields: { MerchantID: '1' },
    })
    const w = await mountOrder()
    await w.find('button.primary').trigger('click')
    await flushPromises()
    expect(payOrder).toHaveBeenCalledWith('AR261005-ABC234', 'ming@example.com')
    expect(submitForm).toHaveBeenCalledWith('https://pay.test/aio', { MerchantID: '1' })
    expect(w.find('button.primary').attributes('disabled')).toBeDefined()
  })

  it('模擬模式導向模擬頁', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    vi.mocked(payOrder).mockResolvedValue({ mode: 'FAKE', tradeNo: 'AR26100501', action: null, fields: null })
    const w = await mountOrder()
    await w.find('button.primary').trigger('click')
    await flushPromises()
    expect(submitForm).not.toHaveBeenCalled()
    expect(w.vm.$router.currentRoute.value.fullPath).toBe('/pay/fake?order=AR261005-ABC234&trade=AR26100501')
  })

  it('開始付款失敗顯示原因並可再試', async () => {
    vi.mocked(getOrder).mockResolvedValue(order())
    vi.mocked(payOrder).mockRejectedValue(new ApiError(500, 'UNKNOWN'))
    const w = await mountOrder()
    await w.find('button.primary').trigger('click')
    await flushPromises()
    expect(w.find('[role="alert"]').exists()).toBe(true)
    expect(w.find('button.primary').attributes('disabled')).toBeUndefined()
  })

  it('上次付款失敗會提示', async () => {
    vi.mocked(getOrder).mockResolvedValue(
      order({ payment: { status: 'FAILED', cardLast4: null, failureCode: 'ECPAY_1', createdAt: '' } }),
    )
    const w = await mountOrder()
    expect(w.text()).toContain('上一次付款沒有成功')
  })

  it('已付款顯示成功畫面並隱藏付款區', async () => {
    vi.mocked(getOrder).mockResolvedValue(
      order({ status: 'PAID', paidAt: new Date().toISOString(), payment: { status: 'SUCCEEDED', cardLast4: '4242', failureCode: null, createdAt: '' } }),
    )
    const w = await mountOrder()
    expect(w.text()).toContain('付款成功')
    expect(w.text()).toContain('末四碼 4242')
    expect(w.find('button.primary').exists()).toBe(false)
  })

  it('付款逾期會重新載入訂單', async () => {
    vi.mocked(getOrder)
      .mockResolvedValueOnce(order())
      .mockResolvedValueOnce(order({ status: 'CANCELLED', cancelReason: 'EXPIRED' }))
    vi.mocked(payOrder).mockRejectedValue(new ApiError(410, 'ORDER_EXPIRED'))
    const w = await mountOrder()
    await w.find('button.primary').trigger('click')
    await flushPromises()
    expect(getOrder).toHaveBeenCalledTimes(2)
    expect(w.text()).toContain('付款逾時，訂單已自動取消')
    expect(w.find('button.primary').exists()).toBe(false)
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

  it('已出貨會顯示物流單號', async () => {
    vi.mocked(getOrder).mockResolvedValue(order({ status: 'SHIPPED', trackingNo: 'TW123' }))
    const w = await mountOrder()
    expect(w.text()).toContain('訂單已出貨')
    expect(w.text()).toContain('TW123')
  })

  it('店家取消顯示對應說明', async () => {
    vi.mocked(getOrder).mockResolvedValue(order({ status: 'CANCELLED', cancelReason: 'STAFF' }))
    const w = await mountOrder()
    expect(w.text()).toContain('由店家取消')
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

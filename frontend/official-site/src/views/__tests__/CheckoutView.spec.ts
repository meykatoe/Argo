import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'
import { useCartStore } from '@/stores/cart'
import type { CardSummary } from '@/types/card'
import { getOrderEmail } from '@/utils/orderAccess'
import CheckoutView from '../CheckoutView.vue'

vi.mock('@/api/card', () => ({ getCardsByIds: vi.fn() }))
vi.mock('@/api/order', () => ({ createOrder: vi.fn() }))
import { getCardsByIds } from '@/api/card'
import { createOrder } from '@/api/order'

function card(id: number, extra: Partial<CardSummary> = {}): CardSummary {
  return {
    id,
    cardSetId: `OP01-00${id}`,
    cardImageId: `OP01-00${id}`,
    setId: 'OP-01',
    cardName: `卡片${id}`,
    cardNameEn: `Card ${id}`,
    rarity: 'C',
    cardColor: 'Red',
    cardType: 'Character',
    cardCost: null,
    cardPower: null,
    imageUrl: null,
    marketPrice: 2,
    salePrice: 1.5,
    listPrice: 1.5,
    stock: 5,
    ...extra,
  }
}

async function mountCheckout(items: [number, number][]) {
  localStorage.clear()
  sessionStorage.clear()
  const pinia = createPinia()
  setActivePinia(pinia)
  const cart = useCartStore()
  items.forEach(([id, qty]) => cart.add(id, qty))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/checkout', component: CheckoutView },
      { path: '/orders/:orderNo', name: 'orderDetail', component: { template: '<i/>' } },
      { path: '/cart', component: { template: '<i/>' } },
      { path: '/cards', component: { template: '<i/>' } },
    ],
  })
  await router.push('/checkout')
  const w = mount(CheckoutView, { global: { plugins: [pinia, router, i18n] } })
  await flushPromises()
  return { w, cart, router }
}

async function fillValid(w: Awaited<ReturnType<typeof mountCheckout>>['w']) {
  await w.find('#c-name').setValue('王小明')
  await w.find('#c-email').setValue('ming@example.com')
  await w.find('#c-phone').setValue('0912345678')
  await w.find('#c-postal').setValue('100')
  await w.find('#c-city').setValue('台北市')
  await w.find('#c-address').setValue('中正區重慶南路一段 122 號')
}

describe('CheckoutView', () => {
  beforeEach(() => {
    vi.mocked(getCardsByIds).mockReset()
    vi.mocked(createOrder).mockReset()
  })

  it('空購物車顯示提示', async () => {
    const { w } = await mountCheckout([])
    expect(w.text()).toContain('購物車是空的')
    expect(getCardsByIds).not.toHaveBeenCalled()
  })

  it('空白表單送出會顯示錯誤', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1)])
    const { w } = await mountCheckout([[1, 2]])
    await w.find('form').trigger('submit')
    expect(w.findAll('[role="alert"]').length).toBeGreaterThanOrEqual(6)
    expect(w.text()).toContain('Email 格式不正確')
    expect(createOrder).not.toHaveBeenCalled()
  })

  it('成功下單後清空購物車並前往訂單頁', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1), card(2)])
    vi.mocked(createOrder).mockResolvedValue({ orderNo: 'AR261005-ABC234' } as never)
    const { w, cart, router } = await mountCheckout([[1, 2], [2, 1]])
    await fillValid(w)
    await w.find('form').trigger('submit')
    await flushPromises()

    const body = vi.mocked(createOrder).mock.calls[0]![0]
    expect(body.items).toEqual([{ cardId: 1, quantity: 2 }, { cardId: 2, quantity: 1 }])
    expect(body.customer).toEqual({ name: '王小明', email: 'ming@example.com', phone: '0912345678' })
    // 預設收件人同購買人
    expect(body.shipping).toMatchObject({ recipientName: '王小明', recipientPhone: '0912345678', city: '台北市' })
    expect(cart.items).toHaveLength(0)
    expect(router.currentRoute.value.path).toBe('/orders/AR261005-ABC234')
    expect(getOrderEmail('AR261005-ABC234')).toBe('ming@example.com')
  })

  it('收件人不同於購買人時送出收件人資料', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1)])
    vi.mocked(createOrder).mockResolvedValue({ orderNo: 'AR1' } as never)
    const { w } = await mountCheckout([[1, 1]])
    await fillValid(w)
    await w.find('.check input').setValue(false)
    await w.find('form').trigger('submit')
    expect(createOrder).not.toHaveBeenCalled()
    await w.find('#c-rname').setValue('李大華')
    await w.find('#c-rphone').setValue('0987654321')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(vi.mocked(createOrder).mock.calls[0]![0].shipping).toMatchObject({
      recipientName: '李大華',
      recipientPhone: '0987654321',
    })
  })

  it('有缺貨商品時不能送出', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1, { stock: 0 })])
    const { w } = await mountCheckout([[1, 1]])
    expect(w.text()).toContain('購物車內有無法購買的商品')
    expect(w.find('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('庫存不足的錯誤會顯示並保留購物車', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1)])
    vi.mocked(createOrder).mockRejectedValue(new ApiError(409, 'INSUFFICIENT_STOCK', { cardId: '1' }))
    const { w, cart } = await mountCheckout([[1, 1]])
    await fillValid(w)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.text()).toContain('庫存不足')
    expect(cart.items).toHaveLength(1)
    // 重新取得最新資料
    expect(getCardsByIds).toHaveBeenCalledTimes(2)
  })

  it('後端欄位錯誤會標在欄位上', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1)])
    vi.mocked(createOrder).mockRejectedValue(
      new ApiError(400, 'VALIDATION_ERROR', { 'customer.email': 'bad' }),
    )
    const { w } = await mountCheckout([[1, 1]])
    await fillValid(w)
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('#c-email').element.closest('.field')!.classList.contains('invalid')).toBe(true)
  })
})

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import { useCartStore } from '@/stores/cart'
import type { CardSummary } from '@/types/card'
import CartView from '../CartView.vue'

vi.mock('@/api/card', () => ({ getCardsByIds: vi.fn() }))
import { getCardsByIds } from '@/api/card'

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
    salePrice: 1.1,
    listPrice: 1.1,
    onSale: 1 as const,
    stock: 5,
    ...extra,
  }
}

async function mountCart(items: [number, number][]) {
  localStorage.clear()
  const pinia = createPinia()
  setActivePinia(pinia)
  const cartStore = useCartStore()
  items.forEach(([id, qty]) => cartStore.add(id, qty))
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { template: '<i/>' } }] })
  await router.push('/cart')
  const w = mount(CartView, { global: { plugins: [pinia, router, i18n] } })
  await flushPromises()
  return { w, cartStore }
}

describe('CartView', () => {
  beforeEach(() => {
    vi.mocked(getCardsByIds).mockReset()
  })

  it('空購物車不請求資料', async () => {
    const { w } = await mountCart([])
    expect(getCardsByIds).not.toHaveBeenCalled()
    expect(w.text()).toContain('購物車是空的')
  })

  it('顯示明細與合計', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1), card(2)])
    const { w } = await mountCart([[1, 2], [2, 3]])
    expect(w.text()).toContain('卡片1')
    expect(w.text()).toContain('共 5 件')
    // 1.1*2 + 1.1*3 = 5.50
    expect(w.text()).toContain('合計 US$ 5.50')
  })

  it('數量超過庫存會調降並提示', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1, { stock: 2 })])
    const { w, cartStore } = await mountCart([[1, 9]])
    expect(cartStore.qtyOf(1)).toBe(2)
    expect(w.text()).toContain('庫存只剩 2 件，已調整數量')
  })

  it('缺貨與下架商品不計入合計', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1), card(2, { stock: 0 })])
    const { w } = await mountCart([[1, 1], [2, 2], [3, 1]])
    expect(w.text()).toContain('目前無法購買')
    expect(w.text()).toContain('此商品已不存在')
    expect(w.text()).toContain('合計 US$ 1.10')
  })

  it('系列下架的商品顯示原因且不計入合計', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1), card(2, { onSale: 0 })])
    const { w } = await mountCart([[1, 1], [2, 2]])
    expect(w.text()).toContain('此商品所屬系列已下架，暫停販售')
    expect(w.text()).toContain('合計 US$ 1.10')
    expect(w.text()).toContain('卡片2')
  })

  it('增減與移除數量', async () => {
    vi.mocked(getCardsByIds).mockResolvedValue([card(1, { stock: 3 })])
    const { w, cartStore } = await mountCart([[1, 2]])
    await w.find('button[aria-label="增加數量"]').trigger('click')
    expect(cartStore.qtyOf(1)).toBe(3)
    // 已到庫存上限
    expect(w.find('button[aria-label="增加數量"]').attributes('disabled')).toBeDefined()
    await w.find('button[aria-label="減少數量"]').trigger('click')
    expect(cartStore.qtyOf(1)).toBe(2)
    await w.find('button.remove').trigger('click')
    expect(cartStore.items).toHaveLength(0)
    expect(w.text()).toContain('購物車是空的')
  })

  it('載入失敗顯示錯誤', async () => {
    vi.mocked(getCardsByIds).mockRejectedValue(new TypeError('x'))
    const { w } = await mountCart([[1, 1]])
    expect(w.text()).toContain('無法連線到伺服器')
  })
})

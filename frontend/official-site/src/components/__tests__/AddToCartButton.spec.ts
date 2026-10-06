import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { i18n } from '@/i18n'
import { useCartStore } from '@/stores/cart'
import AddToCartButton from '../AddToCartButton.vue'

function render(card: { id: number; stock: number; salePrice: number; onSale?: 0 | 1 }) {
  const pinia = createPinia()
  setActivePinia(pinia)
  return { w: mount(AddToCartButton, { props: { card: { onSale: 1, ...card } }, global: { plugins: [pinia, i18n] } }), cart: useCartStore() }
}

describe('AddToCartButton', () => {
  beforeEach(() => localStorage.clear())

  it('點擊加入購物車', async () => {
    const { w, cart } = render({ id: 1, stock: 3, salePrice: 1 })
    expect(w.text()).toBe('加入購物車')
    await w.trigger('click')
    expect(cart.qtyOf(1)).toBe(1)
    expect(w.text()).toBe('已加入購物車')
  })

  it('缺貨時不可點', () => {
    const { w } = render({ id: 1, stock: 0, salePrice: 1 })
    expect(w.attributes('disabled')).toBeDefined()
    expect(w.text()).toBe('缺貨')
  })

  it('系列下架時不可點並顯示已下架', () => {
    const { w } = render({ id: 1, stock: 5, salePrice: 1, onSale: 0 })
    expect(w.attributes('disabled')).toBeDefined()
    expect(w.text()).toBe('已下架')
  })

  it('未定價時不可點', () => {
    const { w } = render({ id: 1, stock: 5, salePrice: 0 })
    expect(w.attributes('disabled')).toBeDefined()
  })

  it('已達庫存上限不可再加', async () => {
    const { w, cart } = render({ id: 1, stock: 1, salePrice: 1 })
    cart.add(1)
    await w.vm.$nextTick()
    expect(w.attributes('disabled')).toBeDefined()
    expect(w.text()).toBe('已達庫存上限')
  })
})

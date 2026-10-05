import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { MAX_LINE_QTY, useCartStore } from '../cart'

describe('cart store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('加入並累計數量', () => {
    const cart = useCartStore()
    cart.add(1)
    cart.add(1, 2)
    cart.add(2)
    expect(cart.qtyOf(1)).toBe(3)
    expect(cart.totalQty).toBe(4)
    expect(cart.items).toHaveLength(2)
  })

  it('數量不超過上限', () => {
    const cart = useCartStore()
    cart.setQty(1, 500)
    expect(cart.qtyOf(1)).toBe(MAX_LINE_QTY)
  })

  it('數量歸零就移除', () => {
    const cart = useCartStore()
    cart.add(1)
    cart.setQty(1, 0)
    expect(cart.items).toHaveLength(0)
  })

  it('可移除與清空', () => {
    const cart = useCartStore()
    cart.add(1)
    cart.add(2)
    cart.remove(1)
    expect(cart.items.map((i) => i.id)).toEqual([2])
    cart.clear()
    expect(cart.totalQty).toBe(0)
  })

  it('變動後寫入瀏覽器', async () => {
    const cart = useCartStore()
    cart.add(7, 2)
    await Promise.resolve()
    expect(JSON.parse(localStorage.getItem('argo.cart')!)).toEqual([{ id: 7, qty: 2 }])
  })

  it('重新載入還原內容', () => {
    localStorage.setItem('argo.cart', JSON.stringify([{ id: 5, qty: 3 }]))
    setActivePinia(createPinia())
    expect(useCartStore().qtyOf(5)).toBe(3)
  })

  it('忽略損毀的資料', () => {
    localStorage.setItem('argo.cart', '{壞掉')
    setActivePinia(createPinia())
    expect(useCartStore().items).toEqual([])

    localStorage.setItem(
      'argo.cart',
      JSON.stringify([{ id: 'x', qty: 1 }, { id: 1, qty: -2 }, { id: 2, qty: 1000 }, null]),
    )
    setActivePinia(createPinia())
    expect(useCartStore().items).toEqual([{ id: 2, qty: MAX_LINE_QTY }])
  })
})

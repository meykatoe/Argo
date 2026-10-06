import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import CardRow from '../CardRow.vue'
import type { AdminCard } from '@/types'

const card: AdminCard = {
  id: 7,
  cardSetId: 'OP01-001',
  cardName: 'Luffy',
  rarity: 'L',
  imageUrl: null,
  marketPrice: 10,
  listPrice: 9,
  extraDiscount: 1,
  salePrice: 9,
  priceOverridden: false,
  stock: 3,
}

function render(props: Partial<AdminCard> = {}) {
  return mount(CardRow, {
    props: { card: { ...card, ...props }, token: 'tk' },
    global: { stubs: {} },
    attachTo: document.createElement('tbody'),
  })
}

afterEach(() => vi.unstubAllGlobals())

describe('CardRow', () => {
  it('送出折扣並帶令牌', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ ...card, extraDiscount: 0.4, salePrice: 3.6 }),
    })
    vi.stubGlobal('fetch', fetchMock)
    const w = render()
    await w.find('input').setValue('0.4')
    await w.find('button').trigger('click')
    await flushPromises()
    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toContain('/admin/cards/7/extra-discount')
    expect(init.method).toBe('PATCH')
    expect(init.headers['X-Admin-Token']).toBe('tk')
    expect(JSON.parse(init.body)).toEqual({ extraDiscount: 0.4 })
    expect(w.emitted('saved')![0]![0]).toMatchObject({ salePrice: 3.6 })
  })

  it('非法輸入不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const w = render()
    await w.find('input').setValue('2')
    await w.find('button').trigger('click')
    expect(fetchMock).not.toHaveBeenCalled()
    expect(w.text()).toContain('請輸入')
  })

  it('手動定價時停用', () => {
    const w = render({ priceOverridden: true })
    expect(w.find('input').attributes('disabled')).toBeDefined()
    expect(w.find('button').attributes('disabled')).toBeDefined()
  })

  it('401 通知登出', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ code: 'ADMIN_UNAUTHORIZED' }) }),
    )
    const w = render()
    await w.find('input').setValue('0.5')
    await w.find('button').trigger('click')
    await flushPromises()
    expect(w.emitted('unauthorized')).toHaveLength(1)
  })
})

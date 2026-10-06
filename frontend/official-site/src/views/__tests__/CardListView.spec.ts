import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import { i18n } from '@/i18n'
import CardListView from '../CardListView.vue'

vi.mock('@/api/card', () => ({
  searchCards: vi.fn(),
  listSets: vi.fn().mockResolvedValue([]),
}))
import { searchCards } from '@/api/card'

const card = {
  id: 1,
  cardSetId: 'OP01-001',
  cardImageId: 'OP01-001',
  setId: 'OP-01',
  cardName: 'Zoro',
  cardNameEn: 'Zoro',
  rarity: 'L',
  cardColor: 'Red',
  cardType: 'Leader',
  cardCost: null,
  cardPower: '5000',
  imageUrl: null,
  marketPrice: 1.5,
  salePrice: 1.35,
  listPrice: 1.35,
  stock: 3,
}

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/cards', component: CardListView },
      { path: '/cards/:id', component: { template: '<i/>' } },
    ],
  })
}

describe('CardListView', () => {
  beforeEach(() => {
    vi.mocked(searchCards).mockReset()
    window.scrollTo = vi.fn()
  })

  it('依網址參數查詢並顯示', async () => {
    vi.mocked(searchCards).mockResolvedValue({
      items: [card], page: 2, size: 24, total: 30, totalPages: 2,
    })
    const router = makeRouter()
    router.push('/cards?keyword=zoro&page=2')
    await router.isReady()
    const w = mount(CardListView, { global: { plugins: [router, i18n, createPinia()] } })
    await flushPromises()

    expect(searchCards).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: 'zoro', page: 2, size: 24 }),
    )
    expect(w.text()).toContain('Zoro')
    expect(w.text()).toContain('共 30 張')
  })

  it('無結果顯示提示', async () => {
    vi.mocked(searchCards).mockResolvedValue({
      items: [], page: 1, size: 24, total: 0, totalPages: 0,
    })
    const router = makeRouter()
    router.push('/cards')
    await router.isReady()
    const w = mount(CardListView, { global: { plugins: [router, i18n, createPinia()] } })
    await flushPromises()
    expect(w.text()).toContain('沒有符合條件的卡片')
  })

  it('換頁會更新網址', async () => {
    vi.mocked(searchCards).mockResolvedValue({
      items: [card], page: 1, size: 24, total: 60, totalPages: 3,
    })
    const router = makeRouter()
    router.push('/cards')
    await router.isReady()
    const w = mount(CardListView, { global: { plugins: [router, i18n, createPinia()] } })
    await flushPromises()

    const next = w.findAll('button').find((b) => b.text() === '下一頁')!
    await next.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.page).toBe('2')
  })

  it('失敗顯示錯誤訊息', async () => {
    vi.mocked(searchCards).mockRejectedValue(new ApiError(500, 'ERROR'))
    const router = makeRouter()
    router.push('/cards')
    await router.isReady()
    const w = mount(CardListView, { global: { plugins: [router, i18n, createPinia()] } })
    await flushPromises()
    expect(w.text()).toContain('發生未知錯誤')
  })
})

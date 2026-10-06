import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import type { CardSummary } from '@/types/card'
import CardTile from '../CardTile.vue'

const base: CardSummary = {
  id: 1,
  cardSetId: 'OP01-001',
  cardImageId: 'OP01-001',
  setId: 'OP-01',
  cardName: '羅羅亞・索隆',
  cardNameEn: 'Roronoa Zoro',
  rarity: 'L',
  cardColor: 'Red',
  cardType: 'Leader',
  cardCost: null,
  cardPower: '5000',
  imageUrl: null,
  marketPrice: 10,
  salePrice: 9,
  listPrice: 9,
  onSale: 1 as const,
  stock: 4,
}

function render(card: Partial<CardSummary>) {
  const router = createRouter({ history: createMemoryHistory(), routes: [] })
  return mount(CardTile, {
    props: { card: { ...base, ...card } },
    global: { plugins: [router, i18n, createPinia()] },
  })
}

describe('CardTile', () => {
  it('顯示售價而非市價', () => {
    const w = render({})
    expect(w.text()).toContain('US$ 9.00')
    expect(w.text()).not.toContain('10.00')
    expect(w.find('.badge').exists()).toBe(false)
  })

  it('沒庫存顯示缺貨', () => {
    const w = render({ stock: 0 })
    expect(w.find('.badge').text()).toBe('缺貨')
  })

  it('未定價顯示提示', () => {
    const w = render({ salePrice: 0 })
    expect(w.text()).toContain('尚未定價')
    expect(w.find('.badge').exists()).toBe(true)
  })

  it('中英文名不同時顯示英文', () => {
    expect(render({}).text()).toContain('Roronoa Zoro')
    expect(render({ cardNameEn: '羅羅亞・索隆' }).find('.en').exists()).toBe(false)
  })
})

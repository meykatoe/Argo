import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'
import CardRow from '../CardRow.vue'
import type { AdminCard } from '@/types'

const card: AdminCard = {
  id: 7,
  cardSetId: 'OP01-001',
  cardName: 'Luffy',
  cardNameEn: 'Luffy',
  rarity: 'L',
  imageUrl: 'https://img.example/op01-001.png',
  marketPrice: 10,
  listPrice: 9,
  extraDiscount: 1,
  salePrice: 9,
  priceOverridden: 0,
  stock: 3,
}

let wrapper: ReturnType<typeof mount> | null = null

function render(p: Partial<AdminCard> = {}) {
  wrapper = mount(CardRow, {
    props: { card: { ...card, ...p }, token: 'tk' },
    attachTo: document.body.appendChild(document.createElement('tbody')),
  })
  return wrapper
}

const dialog = () => document.body.querySelector('[role=dialog]')
const backdrop = () => document.body.querySelector('.backdrop') as HTMLElement | null

afterEach(() => {
  wrapper?.unmount()
  wrapper = null
})

describe('卡牌資訊按鈕與卡圖', () => {
  it('預設不顯示卡圖，按鈕為灰色小按鈕', () => {
    const w = render()
    expect(dialog()).toBeNull()
    expect(w.find('button.info').text()).toBe('卡牌資訊')
  })

  it('點擊後顯示該卡卡圖', async () => {
    const w = render()
    await w.find('button.info').trigger('click')
    const img = dialog()!.querySelector('img')!
    expect(img.getAttribute('src')).toBe(card.imageUrl)
    expect(img.getAttribute('alt')).toBe('Luffy')
    expect(img.getAttribute('referrerpolicy')).toBe('no-referrer')
  })

  it('點擊空白處關閉，點圖片不關閉', async () => {
    const w = render()
    await w.find('button.info').trigger('click')
    dialog()!.querySelector('img')!.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await w.vm.$nextTick()
    expect(dialog()).not.toBeNull()
    backdrop()!.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await w.vm.$nextTick()
    expect(dialog()).toBeNull()
  })

  it('按 Esc 也能關閉', async () => {
    const w = render()
    await w.find('button.info').trigger('click')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await w.vm.$nextTick()
    expect(dialog()).toBeNull()
  })

  it('沒有圖片時顯示提示', async () => {
    const w = render({ imageUrl: null })
    await w.find('button.info').trigger('click')
    expect(dialog()!.textContent).toContain('這張卡沒有圖片')
    expect(dialog()!.querySelector('img')).toBeNull()
  })

  it('開啟卡圖不會觸發儲存', async () => {
    const w = render()
    await w.find('button.info').trigger('click')
    expect(w.emitted('saved')).toBeUndefined()
  })
})

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import Sparkline from '@/components/Sparkline.vue'

const base = {
  label: 'CPU 使用率',
  values: [10, 20, 30, 40],
  times: [1_000_000, 1_010_000, 1_020_000, 1_030_000],
  unit: '%',
  max: 100,
}

describe('Sparkline', () => {
  it('畫出折線與最新一筆的端點，預設不顯示提示', () => {
    const w = mount(Sparkline, { props: base })
    expect(w.find('polyline').exists()).toBe(true)
    expect(w.findAll('circle')).toHaveLength(2)
    expect(w.find('.tip').exists()).toBe(false)
  })

  it('給螢幕閱讀器完整的文字摘要', () => {
    const w = mount(Sparkline, { props: base })
    const label = w.find('svg').attributes('aria-label')!
    expect(label).toContain('CPU 使用率')
    expect(label).toContain('最低 10%')
    expect(label).toContain('最高 40%')
  })

  it('鍵盤可以逐筆移動並顯示該筆數值', async () => {
    const w = mount(Sparkline, { props: base })
    const svg = w.find('svg')
    await svg.trigger('keydown', { key: 'ArrowLeft' })
    expect(w.find('.tip strong').text()).toBe('30%')
    await svg.trigger('keydown', { key: 'ArrowLeft' })
    expect(w.find('.tip strong').text()).toBe('20%')
    await svg.trigger('keydown', { key: 'ArrowRight' })
    expect(w.find('.tip strong').text()).toBe('30%')
    await svg.trigger('keydown', { key: 'Escape' })
    expect(w.find('.tip').exists()).toBe(false)
  })

  it('移到頭尾不會超出範圍', async () => {
    const w = mount(Sparkline, { props: base })
    const svg = w.find('svg')
    for (let i = 0; i < 10; i++) await svg.trigger('keydown', { key: 'ArrowLeft' })
    expect(w.find('.tip strong').text()).toBe('10%')
    for (let i = 0; i < 10; i++) await svg.trigger('keydown', { key: 'ArrowRight' })
    expect(w.find('.tip strong').text()).toBe('40%')
  })

  it('沒有資料時顯示收集中而不是壞掉', () => {
    const w = mount(Sparkline, { props: { ...base, values: [], times: [] } })
    expect(w.text()).toContain('資料收集中')
    expect(w.find('polyline').exists()).toBe(false)
    expect(w.find('svg').attributes('aria-label')).toContain('尚無資料')
  })

  it('只有一筆時只畫端點不畫線', () => {
    const w = mount(Sparkline, { props: { ...base, values: [5], times: [1_000_000] } })
    expect(w.find('polyline').exists()).toBe(false)
    expect(w.findAll('circle')).toHaveLength(2)
  })
})

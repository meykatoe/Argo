import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PriceTag from '../PriceTag.vue'

describe('PriceTag', () => {
  it('無折扣只顯示售價', () => {
    const w = mount(PriceTag, { props: { salePrice: 9, listPrice: 9 } })
    expect(w.find('del').exists()).toBe(false)
    expect(w.text()).not.toContain('SALE!!')
    expect(w.text()).toContain('9.00')
  })

  it('有折扣顯示劃線原價與標示', () => {
    const w = mount(PriceTag, { props: { salePrice: 3.6, listPrice: 9 } })
    expect(w.find('del').text()).toContain('9.00')
    expect(w.find('.now').text()).toContain('3.60')
    expect(w.find('.flag').text()).toBe('(SALE!!)')
  })
})

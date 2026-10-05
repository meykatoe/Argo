import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppPagination from '../AppPagination.vue'

describe('AppPagination', () => {
  it('單頁時不顯示', () => {
    const w = mount(AppPagination, { props: { page: 1, totalPages: 1 } })
    expect(w.find('nav').exists()).toBe(false)
  })

  it('第一頁禁用上一頁', () => {
    const w = mount(AppPagination, { props: { page: 1, totalPages: 5 } })
    expect(w.findAll('button')[0]!.attributes('disabled')).toBeDefined()
  })

  it('點下一頁送出頁碼', async () => {
    const w = mount(AppPagination, { props: { page: 3, totalPages: 10 } })
    const next = w.findAll('button').find((b) => b.text() === '下一頁')!
    await next.trigger('click')
    expect(w.emitted('change')![0]).toEqual([4])
  })
})

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { i18n } from '@/i18n'
import AppPagination from '../AppPagination.vue'

describe('AppPagination', () => {
  it('單頁時不顯示', () => {
    const w = mount(AppPagination, { props: { page: 1, totalPages: 1 }, global: { plugins: [i18n] } })
    expect(w.find('nav').exists()).toBe(false)
  })

  it('第一頁禁用上一頁', () => {
    const w = mount(AppPagination, { props: { page: 1, totalPages: 5 }, global: { plugins: [i18n] } })
    expect(w.findAll('button')[0]!.attributes('disabled')).toBeDefined()
  })

  it('點下一頁送出頁碼', async () => {
    const w = mount(AppPagination, { props: { page: 3, totalPages: 10 }, global: { plugins: [i18n] } })
    const next = w.findAll('button').find((b) => b.text() === '下一頁')!
    await next.trigger('click')
    expect(w.emitted('change')![0]).toEqual([4])
  })
})

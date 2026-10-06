import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import CardSetTable from '../CardSetTable.vue'

const sets = [
  { setId: 'OP-01', setName: '補充包 ROMANCE DAWN', setNameEn: 'Romance Dawn', category: 'booster', onSale: 1, cardCount: 120, minDiscount: 1, maxDiscount: 1 },
  { setId: 'ST-01', setName: '起始牌組 草帽一行', setNameEn: 'Straw Hat Crew', category: 'starter', onSale: 0, cardCount: 17, minDiscount: 1, maxDiscount: 1 },
]

afterEach(() => vi.unstubAllGlobals())

async function render(response: unknown = { ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data: sets }) }) {
  const fetchMock = vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fetchMock)
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { render: () => null } }] })
  await router.push('/series')
  const w = mount(CardSetTable, { props: { token: 'tk' }, global: { plugins: [router] } })
  await flushPromises()
  return { w, fetchMock }
}

describe('CardSetTable', () => {
  it('帶令牌載入並列出所有系列', async () => {
    const { w, fetchMock } = await render()
    expect(fetchMock.mock.calls[0]![0]).toContain('/admin/card-sets')
    expect(fetchMock.mock.calls[0]![1].headers['Authorization']).toBe('Bearer tk')
    expect(w.findAll('tbody tr')).toHaveLength(2)
    expect(w.text()).toContain('共 2 個系列')
  })

  it('可依編號與名稱篩選', async () => {
    const { w } = await render()
    await w.find('input[aria-label=篩選系列]').setValue('straw')
    expect(w.findAll('tbody tr')).toHaveLength(1)
    expect(w.text()).toContain('ST-01')
    await w.find('input[aria-label=篩選系列]').setValue('zzz')
    expect(w.text()).toContain('沒有符合的系列')
  })

  it('401 通知登入失效', async () => {
    const { w } = await render({ ok: false, status: 401, json: async () => ({ code: 401, msg: 'ADMIN_UNAUTHORIZED', data: null }) })
    expect(w.emitted('unauthorized')).toHaveLength(1)
  })
})

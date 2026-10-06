import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import CardTable from '../CardTable.vue'

const options = [
  { setId: 'OP-01', setName: '補充包 ROMANCE DAWN', category: 'booster', onSale: 1 },
  { setId: 'ST-01', setName: '起始牌組 草帽一行', category: 'starter', onSale: 0 },
]

function fetchFor() {
  return vi.fn().mockImplementation(async (url: string) => {
    const data = url.includes('/sets')
      ? options
      : { items: [], page: 1, size: 20, total: 0, totalPages: 0 }
    return { ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) }
  })
}

afterEach(() => vi.unstubAllGlobals())

async function render(path: string) {
  const fetchMock = fetchFor()
  vi.stubGlobal('fetch', fetchMock)
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/cards', component: { render: () => null } }] })
  await router.push(path)
  const w = mount(CardTable, { props: { token: 'tk' }, global: { plugins: [router] } })
  await flushPromises()
  const cardCalls = () => fetchMock.mock.calls.filter((c) => String(c[0]).includes('/admin/cards'))
  const lastCard = () => cardCalls()[cardCalls().length - 1]![0]
  return { w, router, fetchMock, cardCalls, lastCard }
}

describe('CardTable 系列篩選', () => {
  it('下拉選單列出系列，下架的有標示', async () => {
    const { w } = await render('/cards')
    const opts = w.findAll('select[aria-label=系列] option').map((o) => o.text())
    expect(opts).toEqual(['全部系列', 'OP-01 補充包 ROMANCE DAWN', 'ST-01 起始牌組 草帽一行（已下架）'])
  })

  it('網址帶系列時直接套用', async () => {
    const { w, cardCalls } = await render('/cards?setId=OP-01')
    expect(new URL(cardCalls()[0]![0], 'http://x').searchParams.get('setId')).toBe('OP-01')
    expect((w.find('select[aria-label=系列]').element as HTMLSelectElement).value).toBe('OP-01')
  })

  it('選擇系列會重新查詢並寫回網址', async () => {
    const { w, router, cardCalls, lastCard } = await render('/cards')
    expect(new URL(cardCalls()[0]![0], 'http://x').searchParams.has('setId')).toBe(false)
    await w.find('select[aria-label=系列]').setValue('ST-01')
    await flushPromises()
    expect(new URL(lastCard(), 'http://x').searchParams.get('setId')).toBe('ST-01')
    expect(router.currentRoute.value.query.setId).toBe('ST-01')
  })

  it('選回全部系列會拿掉條件', async () => {
    const { w, router, lastCard } = await render('/cards?setId=OP-01')
    await w.find('select[aria-label=系列]').setValue('')
    await flushPromises()
    expect(new URL(lastCard(), 'http://x').searchParams.has('setId')).toBe(false)
    expect(router.currentRoute.value.query.setId).toBeUndefined()
  })

  it('網址從別處改變時同步', async () => {
    const { w, router, lastCard } = await render('/cards')
    await router.push('/cards?setId=ST-01')
    await flushPromises()
    expect((w.find('select[aria-label=系列]').element as HTMLSelectElement).value).toBe('ST-01')
    expect(new URL(lastCard(), 'http://x').searchParams.get('setId')).toBe('ST-01')
  })

  it('系列清單載入失敗不影響卡片列表', async () => {
    const fetchMock = vi.fn().mockImplementation(async (url: string) => {
      if (url.includes('/sets')) throw new TypeError('down')
      return { ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data: { items: [], page: 1, size: 20, total: 0, totalPages: 0 } }) }
    })
    vi.stubGlobal('fetch', fetchMock)
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/cards', component: { render: () => null } }] })
    await router.push('/cards')
    const w = mount(CardTable, { props: { token: 'tk' }, global: { plugins: [router] } })
    await flushPromises()
    expect(w.text()).toContain('沒有符合的卡片')
    expect(w.findAll('select[aria-label=系列] option')).toHaveLength(1)
  })
})

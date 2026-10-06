import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { AdminCardSet } from '@/types'
import CardSetRow from '../CardSetRow.vue'

const set: AdminCardSet = {
  setId: 'OP-01',
  setName: '補充包 ROMANCE DAWN',
  setNameEn: 'Romance Dawn',
  category: 'booster',
  onSale: 1,
  cardCount: 120,
  minDiscount: 1,
  maxDiscount: 1,
}

const ok = (data: unknown) => ({ ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) })

async function render(p: Partial<AdminCardSet> = {}) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { render: () => null } }] })
  await router.push('/series')
  const w = mount(CardSetRow, {
    props: { set: { ...set, ...p }, token: 'tk' },
    global: { plugins: [router] },
    attachTo: document.createElement('tbody'),
  })
  return w
}

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('CardSetRow', () => {
  it('顯示系列資訊與狀態', async () => {
    const w = await render()
    expect(w.text()).toContain('OP-01')
    expect(w.text()).toContain('補充包 ROMANCE DAWN')
    expect(w.text()).toContain('Romance Dawn')
    expect(w.text()).toContain('補充包')
    expect(w.text()).toContain('120')
    expect(w.find('.state').text()).toBe('上架中')
    expect(w.text()).toContain('無折扣')
  })

  it('折扣不一致時顯示範圍', async () => {
    const w = await render({ minDiscount: 0.4, maxDiscount: 1 })
    expect(w.text()).toContain('不一致（4 折 ～ 無折扣）')
  })

  it('下架前會確認，取消則不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false)
    const w = await render()
    await w.findAll('button.small')[0]!.trigger('click')
    expect(confirm).toHaveBeenCalledOnce()
    expect(confirm.mock.calls[0]![0]).toContain('無法購買')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('確認後下架，送出 0', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ ...set, onSale: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const w = await render()
    await w.findAll('button.small')[0]!.trigger('click')
    await flushPromises()
    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toContain('/admin/card-sets/OP-01/on-sale')
    expect(init.method).toBe('PATCH')
    expect(init.headers['Authorization']).toBe('Bearer tk')
    expect(JSON.parse(init.body)).toEqual({ onSale: 0 })
    expect(w.emitted('saved')![0]![0]).toMatchObject({ onSale: 0 })
  })

  it('已下架的系列可重新上架，送出 1', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ ...set, onSale: 1 }))
    vi.stubGlobal('fetch', fetchMock)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const w = await render({ onSale: 0 })
    expect(w.find('.state').text()).toBe('已下架')
    expect(w.findAll('button.small')[0]!.text()).toBe('上架')
    await w.findAll('button.small')[0]!.trigger('click')
    await flushPromises()
    expect(JSON.parse(fetchMock.mock.calls[0]![1].body)).toEqual({ onSale: 1 })
  })

  it('整個系列折扣：確認文字含張數，成功後清空輸入', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ ...set, minDiscount: 0.4, maxDiscount: 0.4 }))
    vi.stubGlobal('fetch', fetchMock)
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const w = await render()
    await w.find('input').setValue('0.4')
    await w.find('button.primary').trigger('click')
    await flushPromises()
    expect(confirm.mock.calls[0]![0]).toContain('120 張')
    expect(confirm.mock.calls[0]![0]).toContain('4 折')
    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toContain('/admin/card-sets/OP-01/extra-discount')
    expect(JSON.parse(init.body)).toEqual({ extraDiscount: 0.4 })
    expect((w.find('input').element as HTMLInputElement).value).toBe('')
  })

  it('折扣輸入不合法不送出也不詢問', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const confirm = vi.spyOn(window, 'confirm')
    const w = await render()
    await w.find('input').setValue('2')
    await w.find('button.primary').trigger('click')
    expect(fetchMock).not.toHaveBeenCalled()
    expect(confirm).not.toHaveBeenCalled()
    expect(w.find('[role=alert]').text()).toContain('請輸入')
  })

  it('401 通知登入失效', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ code: 401, msg: 'ADMIN_UNAUTHORIZED', data: null }) }))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const w = await render()
    await w.findAll('button.small')[0]!.trigger('click')
    await flushPromises()
    expect(w.emitted('unauthorized')).toHaveLength(1)
  })

  it('查看卡片連到該系列的卡牌編輯', async () => {
    const w = await render()
    expect(w.find('a.link').attributes('href')).toBe('/cards?setId=OP-01')
  })
})

import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import RefundList from '@/components/RefundList.vue'

const row = (id: number, extra = {}) => ({
  id, orderNo: `AR-T-${id}`, orderStatus: 'CANCELLED', tradeNo: `T${id}`, amount: 288, currency: 'TWD',
  refundStatus: 'REQUIRED', paidAt: '2026-10-15T02:00:00Z', refundedAt: null, ...extra,
})
const page = (items: unknown[]) => ({ items, page: 1, size: 20, total: items.length, totalPages: 1 })
const ok = (data: unknown) => ({ ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) })
const fail = (status: number, code: number, msg: string) => ({ ok: false, status, json: async () => ({ code, msg, data: null }) })

function setup(impl?: (url: string, init: RequestInit) => unknown) {
  const fetchMock = vi.fn().mockImplementation(async (url: string, init: RequestInit) => impl?.(url, init) ?? ok(page([row(1)])))
  vi.stubGlobal('fetch', fetchMock)
  const w = mount(RefundList, { props: { token: 'tk' }, attachTo: document.body })
  return { w, fetchMock }
}

afterEach(() => {
  document.body.innerHTML = ''
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('RefundList', () => {
  it('預設列出待退款', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    expect(fetchMock.mock.calls[0]![0]).toContain('status=REQUIRED')
    expect(fetchMock.mock.calls[0]![1].headers['Authorization']).toBe('Bearer tk')
    expect(w.text()).toContain('AR-T-1')
    expect(w.find('tbody button').text()).toBe('標記已退款')
  })

  it('切換到已退款不顯示按鈕', async () => {
    const { w, fetchMock } = setup((url) => ok(page(url.includes('REFUNDED') ? [row(2, { refundStatus: 'REFUNDED', refundedAt: '2026-10-15T03:00:00Z' })] : [row(1)])))
    await flushPromises()
    await w.findAll('.bar button')[1]!.trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[fetchMock.mock.calls.length - 1]![0]).toContain('status=REFUNDED')
    expect(w.text()).toContain('AR-T-2')
    expect(w.find('tbody button').exists()).toBe(false)
  })

  it('確認後才送出並重新載入', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { w, fetchMock } = setup((url, init) => (init.method === 'POST' ? ok(row(1, { refundStatus: 'REFUNDED' })) : undefined))
    await flushPromises()
    await w.find('tbody button').trigger('click')
    await flushPromises()
    const post = fetchMock.mock.calls.find((c) => c[1].method === 'POST')!
    expect(post[0]).toContain('/ops/refunds/1/refunded')
  })

  it('取消確認不送出', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.find('tbody button').trigger('click')
    expect(fetchMock.mock.calls.some((c) => c[1].method === 'POST')).toBe(false)
  })

  it('失敗時顯示錯誤文字', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { w } = setup((url, init) => (init.method === 'POST' ? fail(409, 5008, 'REFUND_NOT_REQUIRED') : undefined))
    await flushPromises()
    await w.find('tbody button').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('這筆付款不需要退款')
  })
})

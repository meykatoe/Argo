import { flushPromises, mount } from '@vue/test-utils'
import { computed } from 'vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MENU_CODES } from '@shared/utils/menu'
import OrderDetail from '../OrderDetail.vue'

const base = {
  orderNo: 'A1',
  status: 'PAID',
  currency: 'USD',
  subtotal: 10,
  shippingFee: 2,
  total: 12,
  customerId: 5,
  customerName: '小明',
  customerEmail: 'a@example.test',
  customerPhone: '0900',
  recipientName: '收件人',
  recipientPhone: '0911',
  postalCode: '100',
  city: '台北',
  address: '路1號',
  cancelReason: null,
  createdAt: '2026-01-02T03:04:00Z',
  paidAt: '2026-01-02T03:05:00Z',
  shippedAt: null,
  completedAt: null,
  cancelledAt: null,
  updatedAt: '2026-01-02T03:05:00Z',
  trackingNo: null,
  staffNote: null,
  items: [],
  payment: null,
}

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

function ok(data: unknown) {
  return { ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) }
}

async function render(order: object, codes: string[] = ['order.manage']) {
  const fetchMock = vi.fn().mockResolvedValue(ok(order))
  vi.stubGlobal('fetch', fetchMock)
  const w = mount(OrderDetail, {
    props: { token: 'tk', orderNo: 'A1' },
    attachTo: document.body,
    global: { provide: { [MENU_CODES as symbol]: computed(() => new Set(codes)) } },
  })
  await flushPromises()
  return { w, fetchMock }
}

const q = (sel: string) => document.body.querySelector(sel) as HTMLElement | null
const btn = (text: string) =>
  [...document.body.querySelectorAll('button')].find((b) => b.textContent?.includes(text)) as HTMLElement | undefined

describe('OrderDetail 操作', () => {
  it('沒有權限旗標就不顯示操作', async () => {
    const { w } = await render(base, [])
    expect(btn('出貨')).toBeUndefined()
    expect(btn('取消訂單')).toBeUndefined()
    w.unmount()
  })

  it('已付款可出貨並送出單號', async () => {
    const { w, fetchMock } = await render(base)
    ;(q('input[aria-label=物流單號]') as HTMLInputElement).value = ' TW1 '
    q('input[aria-label=物流單號]')!.dispatchEvent(new Event('input'))
    fetchMock.mockResolvedValue(ok({ ...base, status: 'SHIPPED', trackingNo: 'TW1' }))
    q('form')!.dispatchEvent(new Event('submit'))
    await flushPromises()
    const call = fetchMock.mock.calls[1]!
    expect(call[0]).toContain('/admin/orders/A1/ship')
    expect(JSON.parse(call[1].body)).toEqual({ trackingNo: 'TW1' })
    expect(document.body.textContent).toContain('已出貨')
    expect(w.emitted('changed')).toHaveLength(1)
    w.unmount()
  })

  it('取消前要確認，按否就不送出', async () => {
    const { w, fetchMock } = await render(base)
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    btn('取消訂單')!.click()
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    fetchMock.mockResolvedValue(ok({ ...base, status: 'CANCELLED', cancelReason: 'STAFF' }))
    btn('取消訂單')!.click()
    await flushPromises()
    expect(fetchMock.mock.calls[1]![0]).toContain('/admin/orders/A1/cancel')
    expect(document.body.textContent).toContain('店家取消')
    w.unmount()
  })

  it('已出貨只能標記完成', async () => {
    const { w } = await render({ ...base, status: 'SHIPPED', shippedAt: base.paidAt })
    expect(btn('標記完成')).toBeDefined()
    expect(btn('出貨')).toBeUndefined()
    expect(btn('取消訂單')).toBeUndefined()
    w.unmount()
  })

  it('操作失敗顯示錯誤並保留畫面', async () => {
    const { w, fetchMock } = await render(base)
    fetchMock.mockResolvedValue({ ok: false, status: 409, json: async () => ({ code: 4009, msg: 'ORDER_STATE_CONFLICT', data: null }) })
    q('form')!.dispatchEvent(new Event('submit'))
    await flushPromises()
    expect(document.body.textContent).toContain('訂單狀態已變更')
    expect(document.body.textContent).toContain('收件人')
    w.unmount()
  })
})

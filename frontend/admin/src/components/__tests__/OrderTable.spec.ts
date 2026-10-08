import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import OrderTable from '../OrderTable.vue'

const row = {
  orderNo: 'A1',
  status: 'PAID',
  currency: 'USD',
  total: 12,
  customerName: '小明',
  customerEmail: 'a@example.test',
  itemCount: 2,
  createdAt: '2026-01-02T03:04:00Z',
  paidAt: null,
}
const page = { items: [row], page: 1, size: 20, total: 1, totalPages: 1 }
const detail = {
  ...row,
  subtotal: 10,
  shippingFee: 2,
  customerId: null,
  customerPhone: '0900',
  recipientName: '收件人',
  recipientPhone: '0911',
  postalCode: '100',
  city: '台北',
  address: '路1號',
  cancelReason: null,
  cancelledAt: null,
  updatedAt: row.createdAt,
  items: [
    { cardId: 1, cardSetId: 'OP01-001', cardName: '魯夫', cardNameEn: 'Luffy', imageUrl: null, unitPrice: 5, quantity: 2, subtotal: 10 },
  ],
  payment: null,
}

afterEach(() => vi.unstubAllGlobals())

function ok(data: unknown) {
  return { ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) }
}

async function render(first: unknown = ok(page)) {
  const fetchMock = vi.fn().mockResolvedValue(first)
  vi.stubGlobal('fetch', fetchMock)
  const w = mount(OrderTable, { props: { token: 'tk' }, attachTo: document.body })
  await flushPromises()
  return { w, fetchMock }
}

describe('OrderTable', () => {
  it('帶令牌載入並列出訂單', async () => {
    const { w, fetchMock } = await render()
    expect(fetchMock.mock.calls[0]![0]).toContain('/admin/orders')
    expect(fetchMock.mock.calls[0]![1].headers['Authorization']).toBe('Bearer tk')
    expect(w.findAll('tbody tr')).toHaveLength(1)
    expect(w.text()).toContain('已付款')
  })

  it('選狀態會帶入查詢參數', async () => {
    const { w, fetchMock } = await render()
    await w.find('select').setValue('SHIPPED')
    await flushPromises()
    expect(fetchMock.mock.calls[1]![0]).toContain('status=SHIPPED')
  })

  it('點查看會載入訂單詳情', async () => {
    const { w, fetchMock } = await render()
    fetchMock.mockResolvedValue(ok(detail))
    await w.find('tbody button').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls[1]![0]).toContain('/admin/orders/A1')
    expect(document.body.textContent).toContain('收件人')
    expect(document.body.textContent).toContain('訪客')
    w.unmount()
  })

  it('401 通知登入失效', async () => {
    const { w } = await render({ ok: false, status: 401, json: async () => ({ code: 401, msg: 'ADMIN_UNAUTHORIZED', data: null }) })
    expect(w.emitted('unauthorized')).toHaveLength(1)
  })
})

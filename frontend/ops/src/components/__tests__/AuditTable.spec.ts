import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AuditTable from '../AuditTable.vue'

const row = {
  id: 9,
  staffId: 2,
  username: 'gen1',
  role: 'GENERAL',
  action: 'CARD_EXTRA_DISCOUNT_UPDATE',
  targetType: 'CARD',
  targetId: '7',
  detail: { cardSetId: 'OP01-001', extraDiscountBefore: '1', extraDiscountAfter: '0.4', salePriceBefore: '9.00', salePriceAfter: '3.60' },
  success: 1,
  ip: '10.0.0.5',
  userAgent: 'UA',
  createdAt: '2026-10-06T03:00:00Z',
}

function page(items: unknown[]) {
  return { ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data: { items, page: 1, size: 50, total: items.length, totalPages: 1 } }) }
}

afterEach(() => vi.unstubAllGlobals())

describe('AuditTable', () => {
  it('載入後顯示紀錄並帶令牌', async () => {
    const fetchMock = vi.fn().mockResolvedValue(page([row]))
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toContain('/ops/audit-logs')
    expect(init.headers['Authorization']).toBe('Bearer tk')
    expect(w.text()).toContain('gen1')
    expect(w.text()).toContain('修改額外折扣')
    expect(w.text()).toContain('折扣 1 → 0.4')
    expect(w.text()).toContain('10.0.0.5')
  })

  it('篩選條件送到後端', async () => {
    const fetchMock = vi.fn().mockResolvedValue(page([]))
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    await w.find('input[placeholder=帳號]').setValue('gen')
    const [actionSel, resultSel] = w.findAll('select')
    await actionSel!.setValue('LOGIN_FAILED')
    await resultSel!.setValue('fail')
    await w.find('form').trigger('submit')
    await flushPromises()
    const url = new URL(fetchMock.mock.calls[1]![0], 'http://x')
    expect(url.searchParams.get('username')).toBe('gen')
    expect(url.searchParams.get('action')).toBe('LOGIN_FAILED')
    expect(url.searchParams.get('success')).toBe('0')
    expect(w.text()).toContain('沒有符合的紀錄')
  })

  it('展開詳情可看 JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(page([row])))
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.find('pre').exists()).toBe(false)
    await w.find('button.link').trigger('click')
    expect(w.find('pre').text()).toContain('"salePriceAfter": "3.60"')
  })

  it('失敗列有標示', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(page([{ ...row, action: 'LOGIN_FAILED', success: 0, detail: { reason: 'BAD_PASSWORD' } }])),
    )
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.find('tr.failed').exists()).toBe(true)
    expect(w.text()).toContain('密碼錯誤')
  })

  it('401 通知登出', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ code: 401, msg: 'ADMIN_UNAUTHORIZED', data: null }) }))
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.emitted('unauthorized')).toHaveLength(1)
  })
})

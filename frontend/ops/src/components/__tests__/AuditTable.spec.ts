import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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

describe('AuditTable 自動更新', () => {
  const api = (fn: ReturnType<typeof vi.fn>) => fn.mock.calls.map((c) => new URL(c[0], 'http://x').searchParams)

  beforeEach(() => {
    localStorage.clear()
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
  })
  afterEach(() => vi.useRealTimers())

  it('第一次載入不帶 refresh，之後每 10 秒自動更新並帶 refresh', async () => {
    const fetchMock = vi.fn().mockResolvedValue(page([row]))
    vi.stubGlobal('fetch', fetchMock)
    mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    expect(api(fetchMock)[0]!.has('refresh')).toBe(false)
    vi.advanceTimersByTime(10_000)
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(api(fetchMock)[1]!.get('refresh')).toBe('1')
  })

  it('新紀錄會自己出現，不用手動刷新', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(page([row])).mockResolvedValue(page([{ ...row, id: 10, username: 'newcomer' }, row]))
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    expect(w.text()).not.toContain('newcomer')
    vi.advanceTimersByTime(10_000)
    await flushPromises()
    expect(w.text()).toContain('newcomer')
    expect(w.text()).toContain('上次更新')
  })

  it('自動更新時不顯示載入中、也不會把畫面清空', async () => {
    let release!: (v: unknown) => void
    const fetchMock = vi.fn().mockResolvedValueOnce(page([row])).mockImplementationOnce(() => new Promise((r) => (release = r)))
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    vi.advanceTimersByTime(10_000)
    await flushPromises()
    expect(w.text()).not.toContain('載入中')
    expect(w.text()).toContain('gen1')
    release(page([row]))
    await flushPromises()
  })

  it('翻到第二頁就暫停自動更新並提示', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data: { items: [row], page: 1, size: 50, total: 120, totalPages: 3 } }) })
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    await w.findAll('.pager button').find((b) => b.text() === '下一頁')!.trigger('click')
    await flushPromises()
    const before = fetchMock.mock.calls.length
    vi.advanceTimersByTime(30_000)
    await flushPromises()
    expect(fetchMock.mock.calls.length).toBe(before)
    expect(w.text()).toContain('翻到其他頁時暫停自動更新')
  })

  it('關掉開關就不更新，手動重新整理仍可用且不帶 refresh', async () => {
    const fetchMock = vi.fn().mockResolvedValue(page([row]))
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    await w.find('.live input[type=checkbox]').setValue(false)
    vi.advanceTimersByTime(30_000)
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await w.findAll('.live button')[0]!.trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(api(fetchMock)[1]!.has('refresh')).toBe(false)
  })

  it('自動更新遇到 401 會通知登入失效', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(page([row])).mockResolvedValue({ ok: false, status: 401, json: async () => ({ code: 401, msg: 'ADMIN_UNAUTHORIZED', data: null }) })
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(AuditTable, { props: { token: 'tk' } })
    await flushPromises()
    vi.advanceTimersByTime(10_000)
    await flushPromises()
    expect(w.emitted('unauthorized')).toHaveLength(1)
  })
})

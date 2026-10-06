import { flushPromises, mount } from '@vue/test-utils'
import { computed } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MENU_CODES } from '@/utils/menu'
import IpMonitor from '../IpMonitor.vue'

const rows = [
  { ip: '203.0.113.9', rateLimited: 12, loginFailed: 30, blockedHits: 0, firstSeen: '2026-10-06T01:00:00Z', lastSeen: '2026-10-06T03:00:00Z', blocked: 0, blockExpiresAt: null, blockReason: null },
  { ip: '198.51.100.7', rateLimited: 0, loginFailed: 2, blockedHits: 9, firstSeen: '2026-10-05T01:00:00Z', lastSeen: '2026-10-06T02:00:00Z', blocked: 1, blockExpiresAt: null, blockReason: '撞庫' },
]
const blocks = [{ ip: '198.51.100.7', reason: '撞庫', blockedBy: 'ops1', createdAt: '2026-10-06T02:30:00Z', expiresAt: null }]

const ok = (data: unknown) => ({ ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) })
const fail = (status: number, msg: string) => ({ ok: false, status, json: async () => ({ code: status, msg, data: null }) })
const pageOf = (items: unknown[], extra = {}) => ok({ items, page: 1, size: 20, total: items.length, totalPages: 1, ...extra })

function setup(fetchImpl?: (url: string, init: RequestInit) => unknown, can = true) {
  const fetchMock = vi.fn().mockImplementation(async (url: string, init: RequestInit) => {
    if (fetchImpl) {
      const r = fetchImpl(url, init)
      if (r) return r
    }
    if (url.includes('/ops/ip-blocks')) return ok(init.method === 'GET' ? blocks : null)
    return pageOf(rows)
  })
  vi.stubGlobal('fetch', fetchMock)
  const codes = can ? ['security', 'security.ips', 'security.ips.block'] : ['security', 'security.ips']
  const w = mount(IpMonitor, {
    props: { token: 'tk' },
    global: { provide: { [MENU_CODES as symbol]: computed(() => new Set(codes)) } },
    attachTo: document.body,
  })
  return { w, fetchMock }
}

const lastOf = <T,>(list: T[]): T => list[list.length - 1]!

const calls = (fm: ReturnType<typeof vi.fn>, method: string, part: string) =>
  fm.mock.calls.filter((c) => String(c[0]).includes(part) && (c[1]?.method ?? 'GET') === method)

afterEach(() => {
  document.body.innerHTML = ''
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('IpMonitor', () => {
  it('帶令牌載入異常 IP 與封鎖名單', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    const ipCall = calls(fetchMock, 'GET', '/ops/ips')[0]!
    expect(ipCall[1].headers['Authorization']).toBe('Bearer tk')
    expect(new URL(ipCall[0], 'http://x').searchParams.get('days')).toBe('7')
    expect(w.text()).toContain('203.0.113.9')
    expect(w.text()).toContain('30')
    expect(w.text()).toContain('已封鎖（永久）')
    expect(w.text()).toContain('目前封鎖中的 IP')
    expect(w.text()).toContain('ops1')
  })

  it('篩選條件送到後端', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.find('select[aria-label=統計範圍]').setValue('30')
    await w.find('input[aria-label="IP 搜尋"]').setValue('203.0.113')
    await w.find('form.bar').trigger('submit')
    await flushPromises()
    const last = lastOf(calls(fetchMock, 'GET', '/ops/ips'))
    const q = new URL(last[0], 'http://x').searchParams
    expect(q.get('days')).toBe('30')
    expect(q.get('keyword')).toBe('203.0.113')
    expect(q.get('page')).toBe('1')
  })

  it('沒有封鎖權限就看不到任何封鎖或解除按鈕', async () => {
    const { w } = setup(undefined, false)
    await flushPromises()
    expect(w.text()).toContain('203.0.113.9')
    expect(w.findAll('button').map((b) => b.text())).not.toContain('封鎖')
    expect(w.findAll('button').map((b) => b.text())).not.toContain('解除封鎖')
    expect(w.text()).not.toContain('封鎖指定 IP')
  })

  it('從列表封鎖：IP 已帶入不可改，原因必填', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.findAll('button.danger-text')[0]!.trigger('click')
    const dlg = document.body.querySelector('[role=dialog]') as HTMLElement
    expect((dlg.querySelector('input') as HTMLInputElement).value).toBe('203.0.113.9')
    expect((dlg.querySelector('input') as HTMLInputElement).readOnly).toBe(true)
    dlg.dispatchEvent(new Event('submit', { cancelable: true }))
    await flushPromises()
    expect(dlg.textContent).toContain('請填寫封鎖原因')
    expect(calls(fetchMock, 'POST', '/ops/ip-blocks')).toHaveLength(0)
  })

  it('送出封鎖會帶原因與時數並重新載入', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.findAll('button.danger-text')[0]!.trigger('click')
    const dlg = document.body.querySelector('[role=dialog]') as HTMLElement
    const [, reason] = dlg.querySelectorAll('input')
    ;(reason as HTMLInputElement).value = '大量猜密碼'
    reason!.dispatchEvent(new Event('input'))
    const sel = dlg.querySelector('select') as HTMLSelectElement
    sel.value = '24'
    sel.dispatchEvent(new Event('change'))
    dlg.dispatchEvent(new Event('submit', { cancelable: true }))
    await flushPromises()
    const post = calls(fetchMock, 'POST', '/ops/ip-blocks')[0]!
    expect(post[1].headers['Authorization']).toBe('Bearer tk')
    expect(JSON.parse(post[1].body)).toEqual({ ip: '203.0.113.9', reason: '大量猜密碼', hours: 24 })
    expect(document.body.querySelector('[role=dialog]')).toBeNull()
    expect(calls(fetchMock, 'GET', '/ops/ips').length).toBeGreaterThanOrEqual(2)
  })

  it('預設永久封鎖時 hours 為 null', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.findAll('button.danger-text')[0]!.trigger('click')
    const dlg = document.body.querySelector('[role=dialog]') as HTMLElement
    const reason = dlg.querySelectorAll('input')[1] as HTMLInputElement
    reason.value = 'x'
    reason.dispatchEvent(new Event('input'))
    dlg.dispatchEvent(new Event('submit', { cancelable: true }))
    await flushPromises()
    expect(JSON.parse(calls(fetchMock, 'POST', '/ops/ip-blocks')[0]![1].body).hours).toBeNull()
  })

  it('手動封鎖指定 IP，IP 可輸入', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.findAll('button').find((b) => b.text() === '封鎖指定 IP')!.trigger('click')
    const dlg = document.body.querySelector('[role=dialog]') as HTMLElement
    const [ip, reason] = dlg.querySelectorAll('input') as unknown as HTMLInputElement[]
    expect(ip!.readOnly).toBe(false)
    ip!.value = '192.0.2.44'
    ip!.dispatchEvent(new Event('input'))
    reason!.value = '手動'
    reason!.dispatchEvent(new Event('input'))
    dlg.dispatchEvent(new Event('submit', { cancelable: true }))
    await flushPromises()
    expect(JSON.parse(calls(fetchMock, 'POST', '/ops/ip-blocks')[0]![1].body).ip).toBe('192.0.2.44')
  })

  it('後端拒絕時在對話框顯示原因且不關閉', async () => {
    const { w } = setup((url, init) => (init.method === 'POST' ? fail(400, 'PROTECTED_IP') : undefined))
    await flushPromises()
    await w.findAll('button.danger-text')[0]!.trigger('click')
    const dlg = document.body.querySelector('[role=dialog]') as HTMLElement
    const reason = dlg.querySelectorAll('input')[1] as HTMLInputElement
    reason.value = 'x'
    reason.dispatchEvent(new Event('input'))
    dlg.dispatchEvent(new Event('submit', { cancelable: true }))
    await flushPromises()
    expect(document.body.querySelector('[role=dialog]')!.textContent).toContain('這個位址不可封鎖')
  })

  it('點空白處或按 Esc 關閉對話框', async () => {
    const { w } = setup()
    await flushPromises()
    await w.findAll('button.danger-text')[0]!.trigger('click')
    document.body.querySelector('.backdrop')!.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await flushPromises()
    expect(document.body.querySelector('[role=dialog]')).toBeNull()
    await w.findAll('button.danger-text')[0]!.trigger('click')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(document.body.querySelector('[role=dialog]')).toBeNull()
  })

  it('解除封鎖要先確認，確認後帶 ip 參數送出', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const unblock = w.findAll('button').filter((b) => b.text() === '解除封鎖')[0]!
    await unblock.trigger('click')
    expect(calls(fetchMock, 'DELETE', '/ops/ip-blocks')).toHaveLength(0)
    await unblock.trigger('click')
    await flushPromises()
    const del = calls(fetchMock, 'DELETE', '/ops/ip-blocks')[0]!
    expect(new URL(del[0], 'http://x').searchParams.get('ip')).toBe('198.51.100.7')
    expect(confirm).toHaveBeenCalledTimes(2)
  })

  it('401 通知登入失效', async () => {
    const { w } = setup(() => fail(401, 'ADMIN_UNAUTHORIZED'))
    await flushPromises()
    expect(w.emitted('unauthorized')).toBeTruthy()
  })

  it('沒有紀錄時顯示提示', async () => {
    const { w } = setup((url) => (url.includes('/ops/ips') ? pageOf([]) : url.includes('/ip-blocks') ? ok([]) : undefined))
    await flushPromises()
    expect(w.text()).toContain('這段時間沒有異常紀錄')
    expect(w.text()).toContain('目前沒有封鎖中的 IP')
  })

  it('多頁時可翻頁', async () => {
    const { w, fetchMock } = setup((url) => (url.includes('/ops/ips') ? pageOf(rows, { total: 45, totalPages: 3 }) : undefined))
    await flushPromises()
    expect(w.text()).toContain('1 / 3')
    await w.findAll('.pager button')[1]!.trigger('click')
    await flushPromises()
    expect(new URL(lastOf(calls(fetchMock, 'GET', '/ops/ips'))[0], 'http://x').searchParams.get('page')).toBe('2')
  })
})

describe('IpMonitor 自動更新', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] })
  })
  afterEach(() => vi.useRealTimers())

  it('每 10 秒同時更新異常 IP 與封鎖名單', async () => {
    const { fetchMock } = setup()
    await flushPromises()
    const first = fetchMock.mock.calls.length
    vi.advanceTimersByTime(10_000)
    await flushPromises()
    expect(fetchMock.mock.calls.length).toBe(first * 2)
  })

  it('新的異常 IP 會自己出現', async () => {
    let n = 0
    const { w } = setup((url) => {
      if (url.includes('/ops/ips')) {
        n++
        return pageOf(n === 1 ? [rows[0]] : rows)
      }
      return undefined
    })
    await flushPromises()
    const activityRows = () => w.findAll('table')[0]!.findAll('tbody tr')
    expect(activityRows()).toHaveLength(1)
    vi.advanceTimersByTime(10_000)
    await flushPromises()
    expect(activityRows()).toHaveLength(2)
    expect(w.text()).toContain('上次更新')
  })

  it('封鎖視窗開著時不更新，關閉後恢復', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.findAll('button.danger-text')[0]!.trigger('click')
    const before = fetchMock.mock.calls.length
    vi.advanceTimersByTime(30_000)
    await flushPromises()
    expect(fetchMock.mock.calls.length).toBe(before)
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    vi.advanceTimersByTime(10_000)
    await flushPromises()
    expect(fetchMock.mock.calls.length).toBeGreaterThan(before)
  })

  it('關掉開關就不更新', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    await w.find('.live input[type=checkbox]').setValue(false)
    const before = fetchMock.mock.calls.length
    vi.advanceTimersByTime(60_000)
    await flushPromises()
    expect(fetchMock.mock.calls.length).toBe(before)
  })
})

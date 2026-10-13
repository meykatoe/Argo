import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import PermissionManager from '@/components/PermissionManager.vue'

const node = (code: string, title: string, portal: string, parentCode: string | null, group: number, roles: string[], extra = {}) => ({
  code, title, path: group ? null : `/${code}`, portal, parentCode, sortOrder: 10, enabled: 1, group, protectedNode: false, roles, ...extra,
})

const overview = (extra: Record<string, unknown> = {}) => ({
  roles: ['ADMIN', 'GENERAL', 'SERVICE', 'OPS'],
  nodes: [
    node('card', '卡牌管理', 'ADMIN', null, 1, []),
    node('card.edit', '卡牌編輯', 'ADMIN', 'card', 0, ['ADMIN', 'GENERAL'], extra),
    node('system', '系統管理', 'OPS', null, 1, [], { protectedNode: true }),
    node('system.permissions', '選單與權限', 'OPS', 'system', 0, ['OPS'], { protectedNode: true }),
  ],
})

const ok = (data: unknown) => ({ ok: true, status: 200, json: async () => ({ code: 200, msg: 'OK', data }) })
const fail = (status: number, code: number, msg: string) => ({ ok: false, status, json: async () => ({ code, msg, data: null }) })

function setup(impl?: (url: string, init: RequestInit) => unknown) {
  const fetchMock = vi.fn().mockImplementation(async (url: string, init: RequestInit) => impl?.(url, init) ?? ok(overview()))
  vi.stubGlobal('fetch', fetchMock)
  const w = mount(PermissionManager, { props: { token: 'tk' }, attachTo: document.body })
  return { w, fetchMock }
}

const box = (label: string) => document.querySelector<HTMLInputElement>(`input[aria-label="${label}"]`)!

afterEach(() => {
  document.body.innerHTML = ''
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('PermissionManager', () => {
  it('依入口分表並顯示授權狀態', async () => {
    const { w, fetchMock } = setup()
    await flushPromises()
    expect(fetchMock.mock.calls[0]![1].headers['Authorization']).toBe('Bearer tk')
    expect(w.text()).toContain('業務後台')
    expect(w.text()).toContain('運維後台')
    expect(box('一般管理員 使用 卡牌編輯').checked).toBe(true)
    expect(box('客服 使用 卡牌編輯').checked).toBe(false)
    expect(document.querySelector('input[aria-label="運維 使用 卡牌編輯"]')).toBeNull()
    expect(document.querySelector('input[aria-label="最高權限 使用 選單與權限"]')).toBeNull()
  })

  it('授權直接送出，結果換成回傳的最新資料', async () => {
    const { fetchMock } = setup((url, init) => {
      if (init.method === 'PUT') return ok(overview({ roles: ['ADMIN', 'GENERAL', 'SERVICE'] }))
    })
    await flushPromises()
    const el = box('客服 使用 卡牌編輯')
    el.checked = true
    el.dispatchEvent(new Event('change'))
    await flushPromises()
    const call = fetchMock.mock.calls.find((c) => c[1].method === 'PUT')!
    expect(call[0]).toContain('/ops/permissions/card.edit/roles/SERVICE')
    expect(JSON.parse(call[1].body)).toEqual({ granted: 1 })
    expect(box('客服 使用 卡牌編輯').checked).toBe(true)
    expect(document.body.textContent).toContain('已儲存')
  })

  it('撤銷要確認，取消就不送出且還原勾選', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const { fetchMock } = setup()
    await flushPromises()
    const el = box('一般管理員 使用 卡牌編輯')
    el.checked = false
    el.dispatchEvent(new Event('change'))
    await flushPromises()
    expect(fetchMock.mock.calls.filter((c) => c[1].method === 'PUT')).toHaveLength(0)
    expect(el.checked).toBe(true)
  })

  it('停用選單送出 enabled 0', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const { fetchMock } = setup()
    await flushPromises()
    const el = box('啟用 卡牌編輯')
    el.checked = false
    el.dispatchEvent(new Event('change'))
    await flushPromises()
    const call = fetchMock.mock.calls.find((c) => c[1].method === 'PATCH')!
    expect(call[0]).toContain('/ops/permissions/card.edit')
    expect(JSON.parse(call[1].body)).toEqual({ enabled: 0 })
  })

  it('受保護的節點不能操作', async () => {
    setup()
    await flushPromises()
    expect(box('啟用 系統管理').disabled).toBe(true)
    expect(box('啟用 選單與權限').disabled).toBe(true)
    expect(box('運維 使用 選單與權限').disabled).toBe(true)
  })

  it('失敗時顯示錯誤並重新載入', async () => {
    const { fetchMock } = setup((url, init) => {
      if (init.method === 'PUT') return fail(400, 2013, 'ROLE_PORTAL_MISMATCH')
    })
    await flushPromises()
    const el = box('客服 使用 卡牌編輯')
    el.checked = true
    el.dispatchEvent(new Event('change'))
    await flushPromises()
    expect(document.body.textContent).toContain('這個角色不屬於該後台入口')
    expect(fetchMock.mock.calls.filter((c) => (c[1].method ?? 'GET') === 'GET')).toHaveLength(2)
  })

  it('401 會通知登出', async () => {
    const { w } = setup(() => fail(401, 2005, 'ADMIN_UNAUTHORIZED'))
    await flushPromises()
    expect(w.emitted('unauthorized')).toBeTruthy()
  })
})

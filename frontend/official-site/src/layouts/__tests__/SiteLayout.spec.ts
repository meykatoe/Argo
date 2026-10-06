import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import SiteLayout from '../SiteLayout.vue'

const session = { token: 'tok', email: 'a@b.co', name: '小明', expiresAt: '2099-01-01T00:00:00Z' }

async function render() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { template: '<i/>' } }],
  })
  await router.push('/')
  const w = mount(SiteLayout, { global: { plugins: [pinia, router, i18n] } })
  await flushPromises()
  return w
}

beforeEach(() => localStorage.clear())
afterEach(() => vi.unstubAllGlobals())

describe('SiteLayout 帳號區', () => {
  it('訪客顯示登入連結，且不打確認登入的請求', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const w = await render()
    const link = w.find('a.user')
    expect(link.text()).toBe('登入')
    expect(link.attributes('href')).toBe('/login')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('已登入顯示名字並連到我的帳號，啟動時確認令牌', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(session))
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 200, msg: 'OK', data: { email: 'a@b.co', name: '小明' } })))
    vi.stubGlobal('fetch', fetchMock)
    const w = await render()
    const link = w.find('a.user')
    expect(link.text()).toBe('小明')
    expect(link.attributes('href')).toBe('/account')
    expect(String(fetchMock.mock.calls[0]![0])).toContain('/api/auth/me')
  })

  it('令牌已失效會變回登入連結', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(session))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 401, msg: 'UNAUTHORIZED', data: null }), { status: 401 })))
    const w = await render()
    expect(w.find('a.user').text()).toBe('登入')
  })
})

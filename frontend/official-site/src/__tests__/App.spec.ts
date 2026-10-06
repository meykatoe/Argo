import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from '../App.vue'
import { i18n } from '@/i18n'
import { blocked } from '@/utils/access'

async function render() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { template: '<i/>' } }] })
  await router.push('/')
  const w = mount(App, { global: { plugins: [pinia, router, i18n] } })
  await flushPromises()
  return w
}

beforeEach(() => {
  localStorage.clear()
  blocked.value = false
})
afterEach(() => {
  vi.unstubAllGlobals()
  blocked.value = false
})

describe('App 啟動檢查', () => {
  it('沒被封鎖時顯示正常的網站外框', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 200, msg: 'OK', data: null })))
    vi.stubGlobal('fetch', fetchMock)
    const w = await render()
    expect(String(fetchMock.mock.calls[0]![0])).toContain('/api/ping')
    expect(w.find('header').exists()).toBe(true)
    expect(w.text()).not.toContain('無法存取這個網站')
  })

  it('被封鎖時整個網站改顯示封鎖畫面，看不到導覽列', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 403, msg: 'IP_BLOCKED', data: null }), { status: 403 })))
    const w = await render()
    expect(w.text()).toContain('無法存取這個網站')
    expect(w.text()).toContain('網路位址已被限制存取')
    expect(w.find('header').exists()).toBe(false)
    expect(w.find('nav').exists()).toBe(false)
  })

  it('其他頁面的 API 回報被封鎖時也會立刻切換', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 200, msg: 'OK', data: null }))))
    const w = await render()
    expect(w.find('header').exists()).toBe(true)
    blocked.value = true
    await flushPromises()
    expect(w.find('header').exists()).toBe(false)
    expect(w.text()).toContain('無法存取這個網站')
  })

  it('檢查失敗（例如網路問題）不會誤判成被封鎖', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('down')))
    const w = await render()
    expect(w.find('header').exists()).toBe(true)
  })

  it('其他錯誤（例如限速）不會顯示封鎖畫面', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 429, msg: 'RATE_LIMITED', data: { retryAfterSeconds: '5' } }), { status: 429 })))
    const w = await render()
    expect(w.find('header').exists()).toBe(true)
  })
})

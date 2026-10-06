import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { Session } from '@/types'
import AppShell from '../AppShell.vue'

vi.mock('@/pages', () => ({
  pages: { '/audit-logs': { props: ['token'], template: '<div class="audit-page">audit {{ token }}</div>' } },
}))

const session: Session = { token: 'tk', username: 'alice', role: 'OPS', expiresAt: '2099-01-01T00:00:00Z' }

function menuOf(...paths: [string, string, string | null][]) {
  return [
    {
      code: 'g',
      title: '稽核管理',
      path: null,
      children: paths.map(([code, title, path]) => ({ code, title, path, children: [] })),
    },
  ]
}

async function render(path: string, menu: unknown, status = 200) {
  const fetchMock = vi.fn().mockResolvedValue({ ok: status === 200, status, json: async () => menu })
  vi.stubGlobal('fetch', fetchMock)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { render: () => null } }],
  })
  await router.push(path)
  await router.isReady()
  const w = mount(AppShell, { props: { brand: '運維後台', session }, global: { plugins: [router] } })
  await flushPromises()
  return { w, router, fetchMock }
}

afterEach(() => vi.unstubAllGlobals())

describe('AppShell', () => {
  it('帶令牌取得選單並顯示對應頁面與麵包屑', async () => {
    const { w, fetchMock } = await render('/audit-logs', menuOf(['audit.logs', '稽核紀錄', '/audit-logs']))
    expect(fetchMock.mock.calls[0]![0]).toContain('/ops/menu')
    expect(fetchMock.mock.calls[0]![1].headers['Authorization']).toBe('Bearer tk')
    expect(w.find('.audit-page').text()).toBe('audit tk')
    expect(w.find('.crumbs').text()).toContain('稽核管理')
    expect(w.find('[aria-current=page]').text()).toBe('稽核紀錄')
  })

  it('首頁導向第一個可用頁面', async () => {
    const { router } = await render('/', menuOf(['audit.logs', '稽核紀錄', '/audit-logs']))
    expect(router.currentRoute.value.path).toBe('/audit-logs')
  })

  it('沒有授權的網址不會顯示頁面', async () => {
    const { w, router } = await render('/audit-logs', menuOf(['audit.logs', '稽核紀錄', '/audit-logs']))
    expect(router.currentRoute.value.path).toBe('/audit-logs')
    expect(w.find('.audit-page').exists()).toBe(true)
  })

  it('選單有但頁面尚未實作時顯示提示', async () => {
    const { w } = await render('/settings', menuOf(['audit.settings', '稽核設定', '/settings']))
    expect(w.text()).toContain('此功能尚未開放')
  })

  it('沒有任何選單顯示無可用功能', async () => {
    const { w } = await render('/', [])
    expect(w.text()).toContain('目前尚無可用功能')
  })

  it('選單 401 通知登入失效', async () => {
    const { w } = await render('/', { code: 'ADMIN_UNAUTHORIZED' }, 401)
    expect(w.emitted('expired')).toHaveLength(1)
  })

  it('漢堡按鈕開關抽屜', async () => {
    const { w } = await render('/audit-logs', menuOf(['audit.logs', '稽核紀錄', '/audit-logs']))
    expect(w.find('.shell').classes()).not.toContain('open')
    await w.find('button.burger').trigger('click')
    expect(w.find('.shell').classes()).toContain('open')
  })
})

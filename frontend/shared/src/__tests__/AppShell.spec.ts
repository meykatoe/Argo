import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '../api'
import AppShell from '../components/AppShell.vue'
import type { Session } from '../types'
import { fakeConfig } from './support'

const pages = { '/cards': { props: ['token'], template: '<div class="cards-page">cards {{ token }}</div>' } }

const session: Session = { token: 'tk', username: 'alice', role: 'GENERAL', expiresAt: '2099-01-01T00:00:00Z' }

function menuOf(...paths: [string, string, string | null][]) {
  return [
    {
      code: 'g',
      title: '卡牌管理',
      path: null,
      children: paths.map(([code, title, path]) => ({ code, title, path, children: [] })),
    },
  ]
}

async function render(path: string, menu: unknown, status = 200) {
  const getMenu =
    status === 200
      ? vi.fn().mockResolvedValue(menu)
      : vi.fn().mockRejectedValue(new ApiError(status, 'ADMIN_UNAUTHORIZED'))
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { render: () => null } }],
  })
  await router.push(path)
  await router.isReady()
  const w = mount(AppShell, { props: { config: fakeConfig({ getMenu }, pages), session }, global: { plugins: [router] } })
  await flushPromises()
  return { w, router, getMenu }
}

describe('AppShell', () => {
  it('帶令牌取得選單並顯示對應頁面與麵包屑', async () => {
    const { w, getMenu } = await render('/cards', menuOf(['card.edit', '卡牌編輯', '/cards']))
    expect(getMenu).toHaveBeenCalledWith('tk')
    expect(w.find('.cards-page').text()).toBe('cards tk')
    expect(w.find('.crumbs').text()).toContain('卡牌管理')
    expect(w.find('.crumbs [aria-current=page]').text()).toBe('卡牌編輯')
  })

  it('首頁導向第一個可用頁面', async () => {
    const { router } = await render('/', menuOf(['card.edit', '卡牌編輯', '/cards']))
    expect(router.currentRoute.value.path).toBe('/cards')
  })

  it('沒有授權的網址不會顯示頁面', async () => {
    const { w, router } = await render('/audit-logs', menuOf(['card.edit', '卡牌編輯', '/cards']))
    expect(router.currentRoute.value.path).toBe('/cards')
    expect(w.find('.cards-page').exists()).toBe(true)
  })

  it('選單有但頁面尚未實作時顯示提示', async () => {
    const { w } = await render('/series', menuOf(['card.series', '卡牌系列', '/series']))
    expect(w.text()).toContain('此功能尚未開放')
  })

  it('沒有任何選單顯示無可用功能', async () => {
    const { w } = await render('/', [])
    expect(w.text()).toContain('目前尚無可用功能')
  })

  it('選單 401 通知登入失效', async () => {
    const { w } = await render('/', null, 401)
    expect(w.emitted('expired')).toHaveLength(1)
  })

  it('漢堡按鈕開關抽屜', async () => {
    const { w } = await render('/cards', menuOf(['card.edit', '卡牌編輯', '/cards']))
    expect(w.find('.shell').classes()).not.toContain('open')
    await w.find('button.burger').trigger('click')
    expect(w.find('.shell').classes()).toContain('open')
  })
})

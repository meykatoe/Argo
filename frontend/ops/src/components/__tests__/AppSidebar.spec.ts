import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { MenuNode, Session } from '@/types'
import AppSidebar from '../AppSidebar.vue'

const session: Session = { token: 't', username: 'alice', role: 'OPS', expiresAt: '2099-01-01T00:00:00Z' }
const menu: MenuNode[] = [
  {
    code: 'audit',
    title: '稽核管理',
    path: null,
    children: [
      { code: 'audit.settings', title: '稽核設定', path: '/settings', children: [] },
      { code: 'audit.logs', title: '稽核紀錄', path: '/audit-logs', children: [] },
    ],
  },
]

async function render(path: string, m: MenuNode[] = menu) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { render: () => null } }],
  })
  await router.push(path)
  await router.isReady()
  return mount(AppSidebar, { props: { brand: '運維後台', session, menu: m }, global: { plugins: [router] } })
}

describe('AppSidebar', () => {
  it('顯示品牌、登入者、角色與選單', async () => {
    const w = await render('/audit-logs')
    expect(w.text()).toContain('運維後台')
    expect(w.text()).toContain('alice')
    expect(w.text()).toContain('運維')
    expect(w.text()).toContain('稽核管理')
    expect(w.text()).toContain('稽核設定')
    expect(w.text()).toContain('稽核紀錄')
  })

  it('目前頁面高亮', async () => {
    const w = await render('/audit-logs')
    const active = w.findAll('a.router-link-exact-active')
    expect(active).toHaveLength(1)
    expect(active[0]!.text()).toBe('稽核紀錄')
  })

  it('群組可收合與展開', async () => {
    const w = await render('/audit-logs')
    const btn = w.find('button.group')
    expect(btn.attributes('aria-expanded')).toBe('true')
    await btn.trigger('click')
    expect(btn.attributes('aria-expanded')).toBe('false')
    expect(w.find('ul.sub').attributes('style')).toContain('display: none')
    await btn.trigger('click')
    expect(btn.attributes('aria-expanded')).toBe('true')
  })

  it('沒有選單時顯示提示', async () => {
    const w = await render('/', [])
    expect(w.text()).toContain('目前尚無可用功能')
  })

  it('點登出與點連結會通知外層', async () => {
    const w = await render('/audit-logs')
    await w.find('button.logout').trigger('click')
    await w.find('a').trigger('click')
    expect(w.emitted('logout')).toHaveLength(1)
    expect(w.emitted('navigate')).toHaveLength(1)
  })
})

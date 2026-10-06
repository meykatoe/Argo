import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { MenuNode, Session } from '@/types'
import AppSidebar from '../AppSidebar.vue'

const session: Session = { token: 't', username: 'alice', role: 'GENERAL', expiresAt: '2099-01-01T00:00:00Z' }
const menu: MenuNode[] = [
  {
    code: 'card',
    title: '卡牌管理',
    path: null,
    children: [
      { code: 'card.series', title: '卡牌系列', path: '/series', children: [] },
      { code: 'card.edit', title: '卡牌編輯', path: '/cards', children: [] },
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
  return mount(AppSidebar, { props: { brand: '業務後台', session, menu: m }, global: { plugins: [router] } })
}

describe('AppSidebar', () => {
  it('顯示品牌、登入者、角色與選單', async () => {
    const w = await render('/cards')
    expect(w.text()).toContain('業務後台')
    expect(w.text()).toContain('alice')
    expect(w.text()).toContain('一般管理員')
    expect(w.text()).toContain('卡牌管理')
    expect(w.text()).toContain('卡牌系列')
    expect(w.text()).toContain('卡牌編輯')
  })

  it('目前頁面高亮', async () => {
    const w = await render('/cards')
    const active = w.findAll('a.router-link-exact-active')
    expect(active).toHaveLength(1)
    expect(active[0]!.text()).toBe('卡牌編輯')
  })

  it('群組可收合與展開', async () => {
    const w = await render('/cards')
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
    const w = await render('/cards')
    await w.find('button.logout').trigger('click')
    await w.find('a').trigger('click')
    expect(w.emitted('logout')).toHaveLength(1)
    expect(w.emitted('navigate')).toHaveLength(1)
  })
})

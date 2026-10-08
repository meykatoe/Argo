import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { MenuNode, Session } from '../types'
import AppSidebar from '../components/AppSidebar.vue'

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
    expect(active[0]!.find('.label').text()).toBe('卡牌編輯')
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

  it('沒有頁面的功能權限不會變成空白選項', async () => {
    const m: MenuNode[] = [
      {
        code: 'card',
        title: '卡牌管理',
        path: null,
        children: [
          { code: 'card.edit', title: '卡牌編輯', path: '/cards', children: [] },
          { code: 'card.flag', title: '隱藏權限', path: null, children: [] },
        ],
      },
    ]
    const w = await render('/cards', m)
    expect(w.findAll('ul.sub li')).toHaveLength(1)
    expect(w.text()).not.toContain('隱藏權限')
  })
})

describe('AppSidebar 滑塊', () => {
  it('目前頁面所在的列有滑塊', async () => {
    const w = await render('/cards')
    await new Promise((r) => setTimeout(r, 0))
    expect(w.find('.slider').exists()).toBe(true)
    expect(w.find('.slider').classes()).not.toContain('hover')
  })

  it('滑鼠移到列上滑塊進入預覽，離開後回復', async () => {
    const w = await render('/cards')
    await new Promise((r) => setTimeout(r, 0))
    await w.find('a[href="/series"]').trigger('pointerover')
    expect(w.find('.slider').classes()).toContain('hover')
    await w.find('nav').trigger('pointerleave')
    expect(w.find('.slider').classes()).not.toContain('hover')
  })

  it('父層與子層使用同一種列樣式', async () => {
    const w = await render('/cards')
    const rows = w.findAll('.row')
    expect(rows).toHaveLength(3)
  })

  it('不在選單內的頁面沒有滑塊', async () => {
    const w = await render('/nowhere')
    await new Promise((r) => setTimeout(r, 0))
    expect(w.find('.slider').exists()).toBe(false)
  })
})

describe('AppSidebar 縮小', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => localStorage.clear())

  it('右上角有縮小按鈕，預設是展開', async () => {
    const w = await render('/cards')
    const btn = w.find('button.collapse')
    expect(btn.attributes('aria-label')).toBe('縮小側邊欄')
    expect(btn.attributes('aria-expanded')).toBe('true')
    expect(w.find('aside').classes()).not.toContain('collapsed')
  })

  it('按下去縮成窄條，再按展開，並記住狀態', async () => {
    const w = await render('/cards')
    await w.find('button.collapse').trigger('click')
    expect(w.find('aside').classes()).toContain('collapsed')
    expect(w.find('button.collapse').attributes('aria-label')).toBe('展開側邊欄')
    expect(localStorage.getItem('argo.sidebar.collapsed')).toBe('1')
    await w.find('button.collapse').trigger('click')
    expect(w.find('aside').classes()).not.toContain('collapsed')
    expect(localStorage.getItem('argo.sidebar.collapsed')).toBe('0')
  })

  it('重新載入時還原上次的狀態', async () => {
    localStorage.setItem('argo.sidebar.collapsed', '1')
    const w = await render('/cards')
    expect(w.find('aside').classes()).toContain('collapsed')
  })

  it('縮小時每個連結仍有名稱，滑過會顯示提示', async () => {
    localStorage.setItem('argo.sidebar.collapsed', '1')
    const w = await render('/cards')
    const link = w.find('a[href="/cards"]')
    expect(link.attributes('aria-label')).toBe('卡牌編輯')
    expect(link.attributes('title')).toBe('卡牌編輯')
    expect(link.find('.short').text()).toBe('卡')
  })

  it('展開時不顯示多餘的提示', async () => {
    const w = await render('/cards')
    expect(w.find('a[href="/cards"]').attributes('title')).toBeUndefined()
  })
})

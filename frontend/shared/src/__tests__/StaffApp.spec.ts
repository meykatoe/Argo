import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import StaffApp from '../components/StaffApp.vue'
import { fakeConfig } from './support'

const session = { token: 'tk', username: 'alice', role: 'GENERAL', expiresAt: '2099-01-01T00:00:00Z' }

async function render(config = fakeConfig()) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { render: () => null } }],
  })
  await router.push('/')
  await router.isReady()
  const w = mount(StaffApp, { props: { config }, global: { plugins: [router] } })
  await flushPromises()
  return { w, config }
}

beforeEach(() => sessionStorage.clear())
afterEach(() => sessionStorage.clear())

describe('StaffApp', () => {
  it('沒有登入資料時顯示登入頁', async () => {
    const { w } = await render()
    expect(w.find('form.login').exists()).toBe(true)
  })

  it('登入後存進 sessionStorage 並顯示外殼', async () => {
    const config = fakeConfig({ login: vi.fn().mockResolvedValue(session) })
    const { w } = await render(config)
    await w.find('input').setValue('alice')
    await w.find('input[type=password]').setValue('x')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(JSON.parse(sessionStorage.getItem('test-session')!).token).toBe('tk')
    expect(w.find('aside').exists()).toBe(true)
    expect(w.text()).toContain('測試後台')
  })

  it('讀得到未過期的登入資料，過期的會忽略', async () => {
    sessionStorage.setItem('test-session', JSON.stringify(session))
    expect((await render()).w.find('aside').exists()).toBe(true)
    sessionStorage.setItem('test-session', JSON.stringify({ ...session, expiresAt: '2000-01-01T00:00:00Z' }))
    expect((await render()).w.find('form.login').exists()).toBe(true)
  })

  it('登出會清除本地資料並通知伺服器', async () => {
    sessionStorage.setItem('test-session', JSON.stringify(session))
    const { w, config } = await render()
    await w.find('button.logout').trigger('click')
    await flushPromises()
    expect(sessionStorage.getItem('test-session')).toBeNull()
    expect(config.api.logout).toHaveBeenCalledWith('tk')
    expect(w.find('form.login').exists()).toBe(true)
  })

  it('伺服器登出失敗也照樣登出', async () => {
    sessionStorage.setItem('test-session', JSON.stringify(session))
    const { w } = await render(fakeConfig({ logout: vi.fn().mockRejectedValue(new Error('x')) }))
    await w.find('button.logout').trigger('click')
    await flushPromises()
    expect(w.find('form.login').exists()).toBe(true)
  })
})

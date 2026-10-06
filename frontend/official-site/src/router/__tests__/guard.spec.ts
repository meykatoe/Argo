import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import router from '../index'

const session = { token: 'tok', email: 'a@b.co', name: null, expiresAt: '2099-01-01T00:00:00Z' }

beforeEach(async () => {
  localStorage.clear()
  setActivePinia(createPinia())
  await router.push('/')
})

describe('路由守衛', () => {
  it('沒登入進入我的帳號會被導到登入頁並記住原本要去的地方', async () => {
    await router.push('/account')
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/account')
  })

  it('已登入可以進入我的帳號', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(session))
    setActivePinia(createPinia())
    await router.push('/account')
    expect(router.currentRoute.value.name).toBe('account')
  })

  it('已登入不需要再看登入與註冊頁', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(session))
    setActivePinia(createPinia())
    await router.push('/login')
    expect(router.currentRoute.value.name).toBe('account')
    await router.push('/register')
    expect(router.currentRoute.value.name).toBe('account')
  })

  it('訪客仍可逛卡片、結帳與查訂單', async () => {
    for (const name of ['/cards', '/cart', '/checkout', '/orders']) {
      await router.push(name)
      expect(router.currentRoute.value.path).toBe(name)
    }
  })
})

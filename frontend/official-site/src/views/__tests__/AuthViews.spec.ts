import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import { useAuthStore } from '@/stores/auth'
import AccountView from '../AccountView.vue'
import LoginView from '../LoginView.vue'
import RegisterView from '../RegisterView.vue'

const view = { token: 'tok', email: 'a@b.co', username: 'ming', name: '小明', expiresAt: '2099-01-01T00:00:00Z' }
const ok = (data: unknown) => new Response(JSON.stringify({ code: 200, msg: 'OK', data }))
const fail = (status: number, msg: string) => new Response(JSON.stringify({ code: status, msg, data: null }), { status })

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<i/>' } },
      { path: '/login', name: 'login', component: LoginView },
      { path: '/register', name: 'register', component: RegisterView },
      { path: '/account', name: 'account', component: AccountView },
      { path: '/checkout', component: { template: '<i/>' } },
      { path: '/orders/:orderNo', name: 'orderDetail', component: { template: '<i/>' } },
    ],
  })
}

async function mountAt(path: string) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = makeRouter()
  await router.push(path)
  const Comp = router.currentRoute.value.matched[0]!.components!.default as never
  const w = mount(Comp, { global: { plugins: [pinia, router, i18n] } })
  await flushPromises()
  return { w, router, auth: useAuthStore() }
}

beforeEach(() => localStorage.clear())
afterEach(() => vi.unstubAllGlobals())

describe('LoginView', () => {
  it('欄位驗證不通過不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const { w } = await mountAt('/login')
    await w.find('form').trigger('submit')
    expect(fetchMock).not.toHaveBeenCalled()
    expect(w.text()).toContain('此欄位必填')
    expect(w.findAll('[role=alert], .error').length).toBeGreaterThan(0)
  })

  it('成功後導向 redirect 指定的站內頁面', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok(view)))
    const { w, router, auth } = await mountAt('/login?redirect=/checkout')
    await w.find('#lg-account').setValue('a@b.co')
    await w.find('#lg-pw').setValue('password-1234')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(auth.isLoggedIn).toBe(true)
    expect(router.currentRoute.value.path).toBe('/checkout')
  })

  it('redirect 指向外部網址時回首頁', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok(view)))
    const { w, router } = await mountAt('/login?redirect=//evil.example')
    await w.find('#lg-account').setValue('a@b.co')
    await w.find('#lg-pw').setValue('password-1234')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('可以用帳號或 Email 登入，原樣送出', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok(view))
    vi.stubGlobal('fetch', fetchMock)
    const { w } = await mountAt('/login')
    await w.find('#lg-account').setValue(' Ming ')
    await w.find('#lg-pw').setValue('password-1234')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(JSON.parse(fetchMock.mock.calls[0]![1].body)).toEqual({ account: 'Ming', password: 'password-1234' })
  })

  it('帳密錯誤顯示訊息並留在原頁', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(401, 'LOGIN_FAILED')))
    const { w, router, auth } = await mountAt('/login')
    await w.find('#lg-account').setValue('a@b.co')
    await w.find('#lg-pw').setValue('bad-password')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('[role=alert]').text()).toBe('帳號、Email 或密碼錯誤')
    expect(auth.isLoggedIn).toBe(false)
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('帳號被鎖定時顯示要等幾分鐘', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: 429, msg: 'LOGIN_LOCKED', data: { retryAfterSeconds: '872' } }), { status: 429 }),
      ),
    )
    const { w } = await mountAt('/login')
    await w.find('#lg-account').setValue('a@b.co')
    await w.find('#lg-pw').setValue('bad-password')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('[role=alert]').text()).toBe('密碼錯誤次數過多，帳號已暫時鎖定，請 15 分鐘後再試')
  })

  it('鎖定但沒帶秒數時用一般訊息', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(429, 'LOGIN_LOCKED')))
    const { w } = await mountAt('/login')
    await w.find('#lg-account').setValue('a@b.co')
    await w.find('#lg-pw').setValue('bad-password')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('[role=alert]').text()).toBe('嘗試次數過多，請稍後再試')
  })

  it('說明可以不註冊直接以訪客購買', async () => {
    const { w } = await mountAt('/login')
    expect(w.text()).toContain('訪客身分購買')
  })
})

describe('RegisterView', () => {
  async function fill(w: Awaited<ReturnType<typeof mountAt>>['w'], pw: string, confirm: string) {
    // 沒填帳號才補預設值，讓測試可先指定帳號
    if (!(w.find('#rg-username').element as HTMLInputElement).value) {
      await w.find('#rg-username').setValue('ming_01')
    }
    await w.find('#rg-email').setValue('a@b.co')
    await w.find('#rg-pw').setValue(pw)
    await w.find('#rg-confirm').setValue(confirm)
    await w.find('form').trigger('submit')
    await flushPromises()
  }

  it('密碼太短或兩次不一致不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const { w } = await mountAt('/register')
    await fill(w, 'short', 'short')
    expect(w.text()).toContain('密碼至少 8 個字元')
    await fill(w, 'abcdefgh', 'abcdefgh')
    expect(w.text()).toContain('密碼需同時包含英文字母與數字')
    await fill(w, '12345678', '12345678')
    expect(w.text()).toContain('密碼需同時包含英文字母與數字')
    await fill(w, 'password-1234', 'password-9999')
    expect(w.text()).toContain('兩次輸入的密碼不一致')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('超過 72 位元組的密碼不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const { w } = await mountAt('/register')
    const long = '密'.repeat(25)
    await fill(w, long, long)
    expect(w.text()).toContain('密碼太長')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('帳號格式不對不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const { w } = await mountAt('/register')
    for (const bad of ['ab', 'has space', 'a@b.co', '-lead', '中文帳號']) {
      await w.find('#rg-username').setValue(bad)
      await fill(w, 'password-1234', 'password-1234')
      expect(w.text()).toContain('帳號需為 3 到 30 個')
    }
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('離開帳號欄位會檢查，已被使用就提示', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ available: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    const { w } = await mountAt('/register')
    await w.find('#rg-username').setValue('Taken_Name')
    await w.find('#rg-username').trigger('blur')
    await flushPromises()
    expect(String(fetchMock.mock.calls[0]![0])).toContain('/auth/username-available')
    expect(String(fetchMock.mock.calls[0]![0])).toContain('username=Taken_Name')
    expect(w.text()).toContain('這個帳號已經有人使用')
    // 已知被使用就不送出註冊
    await fill(w, 'password-1234', 'password-1234')
    expect(fetchMock.mock.calls.every((c) => String(c[0]).includes('username-available'))).toBe(true)
  })

  it('帳號可用時不顯示錯誤，改過欄位後清除提示', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok({ available: 1 })))
    const { w } = await mountAt('/register')
    await w.find('#rg-username').setValue('free_name')
    await w.find('#rg-username').trigger('blur')
    await flushPromises()
    expect(w.text()).not.toContain('這個帳號已經有人使用')
  })

  it('檢查帳號失敗不擋註冊', async () => {
    const fetchMock = vi.fn().mockRejectedValueOnce(new TypeError('x')).mockResolvedValue(ok(view))
    vi.stubGlobal('fetch', fetchMock)
    const { w, auth } = await mountAt('/register')
    await w.find('#rg-username').trigger('blur')
    await w.find('#rg-username').setValue('ming_01')
    await w.find('#rg-username').trigger('blur')
    await flushPromises()
    await fill(w, 'password-1234', 'password-1234')
    expect(auth.isLoggedIn).toBe(true)
  })

  it('註冊時帳號被搶先使用，標在帳號欄位', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(409, 'USERNAME_TAKEN')))
    const { w, auth } = await mountAt('/register')
    await fill(w, 'password-1234', 'password-1234')
    expect(w.text()).toContain('這個帳號已經有人使用')
    expect(auth.isLoggedIn).toBe(false)
  })

  it('成功後直接登入', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok(view)))
    const { w, auth, router } = await mountAt('/register')
    await fill(w, 'password-1234', 'password-1234')
    expect(auth.isLoggedIn).toBe(true)
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('Email 已註冊顯示提示', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(409, 'EMAIL_TAKEN')))
    const { w, auth } = await mountAt('/register')
    await fill(w, 'password-1234', 'password-1234')
    expect(w.find('[role=alert]').text()).toContain('已經註冊過')
    expect(auth.isLoggedIn).toBe(false)
  })
})

describe('AccountView', () => {
  const order = {
    orderNo: 'AR261005-ABC234', status: 'PENDING_PAYMENT', currency: 'USD', total: 9, createdAt: '2026-10-05T03:00:00Z',
    customerEmail: 'buyer@example.com', items: [{}, {}],
  }

  it('列出我的訂單並帶令牌', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(view))
    const fetchMock = vi.fn().mockResolvedValue(ok({ items: [order], page: 1, size: 10, total: 1, totalPages: 1 }))
    vi.stubGlobal('fetch', fetchMock)
    const { w } = await mountAt('/account')
    expect(String(fetchMock.mock.calls[0]![0])).toContain('/api/me/orders')
    expect(fetchMock.mock.calls[0]![1].headers['Authorization']).toBe('Bearer tok')
    expect(w.text()).toContain('AR261005-ABC234')
    expect(w.text()).toContain('待付款')
    expect(w.text()).toContain('共 2 項商品')
    expect(w.text()).toContain('US$ 9.00')
  })

  it('點訂單會記住下單 Email 並前往訂單頁', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(view))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok({ items: [order], page: 1, size: 10, total: 1, totalPages: 1 })))
    const { w, router } = await mountAt('/account')
    await w.find('button.order').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/orders/AR261005-ABC234')
    expect(JSON.parse(sessionStorage.getItem('argo.orderAccess')!)['AR261005-ABC234']).toBe('buyer@example.com')
  })

  it('沒有訂單顯示提示', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(view))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok({ items: [], page: 1, size: 10, total: 0, totalPages: 0 })))
    const { w } = await mountAt('/account')
    expect(w.text()).toContain('還沒有訂單')
  })

  it('令牌失效會登出並回登入頁', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(view))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(401, 'UNAUTHORIZED')))
    const { router, auth } = await mountAt('/account')
    await flushPromises()
    expect(auth.isLoggedIn).toBe(false)
    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/account')
  })

  it('登出回首頁', async () => {
    localStorage.setItem('argo.auth', JSON.stringify(view))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(ok({ items: [], page: 1, size: 10, total: 0, totalPages: 0 })).mockResolvedValue(ok(null)))
    const { w, router, auth } = await mountAt('/account')
    await w.find('button.out').trigger('click')
    await flushPromises()
    expect(auth.isLoggedIn).toBe(false)
    expect(router.currentRoute.value.path).toBe('/')
  })
})

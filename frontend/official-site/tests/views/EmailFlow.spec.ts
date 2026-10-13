import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { i18n } from '@/i18n'
import { AUTH_KEY } from '@/utils/authToken'
import ForgotPasswordView from '@/views/ForgotPasswordView.vue'
import ResetPasswordView from '@/views/ResetPasswordView.vue'
import VerifyEmailView from '@/views/VerifyEmailView.vue'

const ok = (data: unknown = null) => new Response(JSON.stringify({ code: 200, msg: 'OK', data }))
const fail = (status: number, msg: string, data: unknown = null) =>
  new Response(JSON.stringify({ code: 2000 + status, msg, data }), { status })

function login() {
  localStorage.setItem(
    AUTH_KEY,
    JSON.stringify({ token: 'tok', email: 'a@b.co', username: 'ming', name: null, expiresAt: '2099-01-01T00:00:00Z' }),
  )
}

async function mountAt(path: string) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<i/>' } },
      { path: '/login', name: 'login', component: { template: '<i/>' } },
      { path: '/account', name: 'account', component: { template: '<i/>' } },
      { path: '/forgot-password', name: 'forgotPassword', component: ForgotPasswordView },
      { path: '/reset-password', name: 'resetPassword', component: ResetPasswordView },
      { path: '/verify-email', name: 'verifyEmail', component: VerifyEmailView },
    ],
  })
  await router.push(path)
  const Comp = router.currentRoute.value.matched[0]!.components!.default as never
  const w = mount(Comp, { global: { plugins: [pinia, router, i18n] } })
  await flushPromises()
  return w
}

beforeEach(() => localStorage.clear())
afterEach(() => vi.unstubAllGlobals())

describe('VerifyEmailView', () => {
  it('帶令牌送出驗證，成功就登入', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok({ token: 'tok', email: 'a@b.co', username: 'ming', name: null, expiresAt: '2099-01-01T00:00:00Z' }))
    vi.stubGlobal('fetch', fetchMock)
    const w = await mountAt('/verify-email?token=abc')
    expect(JSON.parse(fetchMock.mock.calls[0]![1].body)).toEqual({ token: 'abc' })
    expect(w.text()).toContain('Email 已驗證完成')
    expect(JSON.parse(localStorage.getItem(AUTH_KEY)!).token).toBe('tok')
  })

  it('令牌無效顯示錯誤且不登入', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(400, 'TOKEN_INVALID')))
    const w = await mountAt('/verify-email?token=bad')
    expect(w.find('[role=alert]').text()).toContain('連結無效')
    expect(localStorage.getItem(AUTH_KEY)).toBeNull()
  })

  it('沒有令牌不送出請求', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const w = await mountAt('/verify-email')
    expect(fetchMock).not.toHaveBeenCalled()
    expect(w.find('[role=alert]').exists()).toBe(true)
  })
})

describe('ForgotPasswordView', () => {
  it('Email 格式不對不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const w = await mountAt('/forgot-password')
    await w.find('#fp-email').setValue('nope')
    await w.find('form').trigger('submit')
    expect(fetchMock).not.toHaveBeenCalled()
    expect(w.text()).toContain('Email 格式不正確')
  })

  it('送出後只顯示通用提示', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok())
    vi.stubGlobal('fetch', fetchMock)
    const w = await mountAt('/forgot-password')
    await w.find('#fp-email').setValue('a@b.co')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(JSON.parse(fetchMock.mock.calls[0]![1].body)).toEqual({ email: 'a@b.co' })
    expect(w.text()).toContain('如果這個 Email 有註冊')
    expect(w.find('form').exists()).toBe(false)
  })
})

describe('ResetPasswordView', () => {
  it('沒有令牌顯示錯誤且沒有表單', async () => {
    const w = await mountAt('/reset-password')
    expect(w.find('form').exists()).toBe(false)
    expect(w.find('[role=alert]').exists()).toBe(true)
  })

  it('密碼不一致不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const w = await mountAt('/reset-password?token=t1')
    await w.find('#rp-pw').setValue('password-1234')
    await w.find('#rp-confirm').setValue('password-9999')
    await w.find('form').trigger('submit')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('成功後清掉本機登入並提示重新登入', async () => {
    login()
    const fetchMock = vi.fn().mockResolvedValue(ok())
    vi.stubGlobal('fetch', fetchMock)
    const w = await mountAt('/reset-password?token=t1')
    await w.find('#rp-pw').setValue('password-1234')
    await w.find('#rp-confirm').setValue('password-1234')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(JSON.parse(fetchMock.mock.calls[0]![1].body)).toEqual({ token: 't1', password: 'password-1234' })
    expect(w.text()).toContain('密碼已更新')
    expect(localStorage.getItem(AUTH_KEY)).toBeNull()
  })

  it('令牌過期顯示錯誤', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(400, 'TOKEN_INVALID')))
    const w = await mountAt('/reset-password?token=old')
    await w.find('#rp-pw').setValue('password-1234')
    await w.find('#rp-confirm').setValue('password-1234')
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(w.find('[role=alert]').text()).toContain('連結無效')
  })
})

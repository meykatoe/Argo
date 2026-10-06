import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import LoginForm from '../LoginForm.vue'

afterEach(() => vi.unstubAllGlobals())

async function fill(w: ReturnType<typeof mount>, user: string, pass: string) {
  const inputs = w.findAll('input')
  await inputs[0]!.setValue(user)
  await inputs[1]!.setValue(pass)
  await w.find('form').trigger('submit')
  await flushPromises()
}

describe('LoginForm', () => {
  it('使用運維登入入口', async () => {
    const session = { token: 't', username: 'ops1', role: 'OPS', expiresAt: '2099-01-01T00:00:00Z' }
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => session })
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(LoginForm)
    await fill(w, 'ops1', 'password-1234')
    expect(fetchMock.mock.calls[0]![0]).toContain('/ops/auth/login')
    expect(w.emitted('login')![0]![0]).toEqual(session)
  })

  it('失敗顯示錯誤', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ code: 'LOGIN_FAILED' }) }))
    const w = mount(LoginForm)
    await fill(w, 'ops1', 'bad')
    expect(w.find('[role=alert]').text()).toBe('帳號或密碼錯誤')
    expect(w.emitted('login')).toBeUndefined()
  })
})

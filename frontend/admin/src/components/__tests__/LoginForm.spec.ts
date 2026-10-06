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
  it('以帳號密碼登入且不帶授權標頭', async () => {
    const session = { token: 't', username: 'alice', role: 'GENERAL', expiresAt: '2099-01-01T00:00:00Z' }
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => session })
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(LoginForm)
    await fill(w, ' alice ', 'password-1234')
    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toContain('/admin/auth/login')
    expect(init.method).toBe('POST')
    expect(init.headers['Authorization']).toBeUndefined()
    expect(JSON.parse(init.body)).toEqual({ username: 'alice', password: 'password-1234' })
    expect(w.emitted('login')![0]![0]).toEqual(session)
  })

  it('失敗顯示錯誤並不觸發登入', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ code: 'LOGIN_FAILED' }) }),
    )
    const w = mount(LoginForm)
    await fill(w, 'alice', 'bad')
    expect(w.find('[role=alert]').text()).toBe('帳號或密碼錯誤')
    expect(w.emitted('login')).toBeUndefined()
  })

  it('鎖定時顯示專屬訊息', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 429, json: async () => ({ code: 'LOGIN_LOCKED' }) }),
    )
    const w = mount(LoginForm)
    await fill(w, 'alice', 'x')
    expect(w.find('[role=alert]').text()).toContain('錯誤次數過多')
  })

  it('空白欄位不送出', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const w = mount(LoginForm)
    await fill(w, '', '')
    expect(fetchMock).not.toHaveBeenCalled()
  })
})

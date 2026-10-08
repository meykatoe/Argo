import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api'
import type { StaffConfig } from '../config'
import LoginForm from '../components/LoginForm.vue'
import { fakeConfig } from './support'

async function fill(w: ReturnType<typeof mount>, user: string, pass: string) {
  const inputs = w.findAll('input')
  await inputs[0]!.setValue(user)
  await inputs[1]!.setValue(pass)
  await w.find('form').trigger('submit')
  await flushPromises()
}

const session = { token: 't', username: 'alice', role: 'GENERAL', expiresAt: '2099-01-01T00:00:00Z' }

function form(login: ReturnType<typeof vi.fn>) {
  return mount(LoginForm, { props: { config: fakeConfig({ login: login as StaffConfig['api']['login'] }) } })
}

describe('LoginForm', () => {
  it('顯示設定的標題', () => {
    expect(form(vi.fn()).find('h1').text()).toBe('Argo 測試')
  })

  it('以帳號密碼登入並去掉前後空白', async () => {
    const login = vi.fn().mockResolvedValue(session)
    const w = form(login)
    await fill(w, ' alice ', 'Password-1234')
    expect(login).toHaveBeenCalledWith('alice', 'Password-1234')
    expect(w.emitted('login')![0]![0]).toEqual(session)
  })

  it('失敗顯示錯誤並不觸發登入', async () => {
    const w = form(vi.fn().mockRejectedValue(new ApiError(401, 'LOGIN_FAILED')))
    await fill(w, 'alice', 'bad')
    expect(w.find('[role=alert]').text()).toBe('帳號或密碼錯誤')
    expect(w.emitted('login')).toBeUndefined()
  })

  it('空白欄位不送出', async () => {
    const login = vi.fn()
    const w = form(login)
    await fill(w, '', '')
    expect(login).not.toHaveBeenCalled()
  })

  it('帳號被鎖定時顯示要等幾分鐘', async () => {
    const w = form(vi.fn().mockRejectedValue(new ApiError(429, 'LOGIN_LOCKED', { retryAfterSeconds: '61' })))
    await fill(w, 'alice', 'bad')
    expect(w.find('[role=alert]').text()).toBe('密碼錯誤次數過多，帳號已暫時鎖定，請 2 分鐘後再試')
  })

  it('沒有等待秒數時顯示一般鎖定訊息', async () => {
    const w = form(vi.fn().mockRejectedValue(new ApiError(429, 'LOGIN_LOCKED')))
    await fill(w, 'alice', 'x')
    expect(w.find('[role=alert]').text()).toContain('錯誤次數過多')
  })

  it('網路錯誤顯示無法連線', async () => {
    const w = form(vi.fn().mockRejectedValue(new TypeError('fail')))
    await fill(w, 'alice', 'x')
    expect(w.find('[role=alert]').text()).toBe('無法連線到伺服器')
  })
})

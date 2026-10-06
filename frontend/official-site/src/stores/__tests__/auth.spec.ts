import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AUTH_KEY, authToken } from '@/utils/authToken'
import { useAuthStore } from '../auth'

const view = { token: 'tok', email: 'a@b.co', name: '小明', expiresAt: '2099-01-01T00:00:00Z' }
const ok = (data: unknown) => new Response(JSON.stringify({ code: 200, msg: 'OK', data }))
const fail = (status: number, msg: string) => new Response(JSON.stringify({ code: status, msg, data: null }), { status })

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
})
afterEach(() => vi.unstubAllGlobals())

describe('auth store', () => {
  it('登入後保存並讓請求帶上令牌', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(ok(view)).mockResolvedValueOnce(ok({ email: 'a@b.co', name: '小明' }))
    vi.stubGlobal('fetch', fetchMock)
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
    await auth.login('a@b.co', 'password-1234')
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.displayName).toBe('小明')
    expect(authToken()).toBe('tok')
    expect(JSON.parse(localStorage.getItem(AUTH_KEY)!).token).toBe('tok')
    await auth.refresh()
    expect(fetchMock.mock.calls[1]![1].headers['Authorization']).toBe('Bearer tok')
  })

  it('登入請求不帶舊令牌以外的內容，失敗不保存', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(401, 'LOGIN_FAILED')))
    const auth = useAuthStore()
    await expect(auth.login('a@b.co', 'bad')).rejects.toMatchObject({ code: 'LOGIN_FAILED' })
    expect(auth.isLoggedIn).toBe(false)
    expect(localStorage.getItem(AUTH_KEY)).toBeNull()
  })

  it('註冊成功直接登入', async () => {
    const fetchMock = vi.fn().mockResolvedValue(ok(view))
    vi.stubGlobal('fetch', fetchMock)
    const auth = useAuthStore()
    await auth.register('a@b.co', 'password-1234', '')
    expect(auth.isLoggedIn).toBe(true)
    expect(JSON.parse(fetchMock.mock.calls[0]![1].body)).toEqual({ email: 'a@b.co', password: 'password-1234', name: null })
  })

  it('沒有名字時顯示 Email', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok({ ...view, name: null })))
    const auth = useAuthStore()
    await auth.login('a@b.co', 'password-1234')
    expect(auth.displayName).toBe('a@b.co')
  })

  it('登出一定清掉本機，即使伺服器失敗', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(ok(view)).mockRejectedValueOnce(new TypeError('down')))
    const auth = useAuthStore()
    await auth.login('a@b.co', 'password-1234')
    await auth.logout()
    expect(auth.isLoggedIn).toBe(false)
    expect(localStorage.getItem(AUTH_KEY)).toBeNull()
  })

  it('啟動確認令牌，失效就登出，網路問題則保留', async () => {
    localStorage.setItem(AUTH_KEY, JSON.stringify(view))
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fail(401, 'UNAUTHORIZED')))
    const a = useAuthStore()
    expect(a.isLoggedIn).toBe(true)
    await a.refresh()
    expect(a.isLoggedIn).toBe(false)

    localStorage.setItem(AUTH_KEY, JSON.stringify(view))
    setActivePinia(createPinia())
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('down')))
    const b = useAuthStore()
    await b.refresh()
    expect(b.isLoggedIn).toBe(true)
  })

  it('讀取時略過已過期或壞掉的資料', () => {
    localStorage.setItem(AUTH_KEY, JSON.stringify({ ...view, expiresAt: '2000-01-01T00:00:00Z' }))
    expect(useAuthStore().isLoggedIn).toBe(false)
    localStorage.setItem(AUTH_KEY, '{oops')
    setActivePinia(createPinia())
    expect(useAuthStore().isLoggedIn).toBe(false)
  })
})

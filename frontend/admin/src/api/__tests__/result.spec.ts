import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, login, logout } from '../admin'

function respond(status: number, body: unknown) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status })))
}

afterEach(() => vi.unstubAllGlobals())

describe('Result 格式', () => {
  it('成功時只回傳 data', async () => {
    respond(200, { code: 200, msg: 'OK', data: { token: 't' } })
    expect(await login('a', 'b')).toEqual({ token: 't' })
  })

  it('沒有資料的成功回應也可用', async () => {
    respond(200, { code: 200, msg: 'OK', data: null })
    await expect(logout('t')).resolves.toBeNull()
  })

  it('失敗時 msg 成為錯誤代碼', async () => {
    respond(401, { code: 401, msg: 'LOGIN_FAILED', data: null })
    const err = (await login('a', 'b').catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(401)
    expect(err.code).toBe('LOGIN_FAILED')
  })

  it('不是 Result 格式時用預設代碼', async () => {
    respond(200, [])
    const err = (await login('a', 'b').catch((e) => e)) as ApiError
    expect(err.code).toBe('ERROR')
  })

  it('不是 JSON 時用預設代碼', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('boom', { status: 500 })))
    const err = (await login('a', 'b').catch((e) => e)) as ApiError
    expect(err.code).toBe('ERROR')
    expect(err.status).toBe(500)
  })
})

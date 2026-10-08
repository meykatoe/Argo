import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, request } from '../api'

function respond(status: number, body: unknown) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status })))
}

afterEach(() => vi.unstubAllGlobals())

describe('request', () => {
  it('成功時只回傳 data', async () => {
    respond(200, { code: 200, msg: 'OK', data: { a: 1 } })
    expect(await request(null, 'GET', '/x')).toEqual({ a: 1 })
  })

  it('帶令牌，是否類參數轉成 1 或 0，略過空值', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 200, msg: 'OK', data: null })))
    vi.stubGlobal('fetch', fetchMock)
    await request('tk', 'GET', '/x', { on: true, off: false, empty: '', none: undefined, n: 3 })
    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toContain('on=1')
    expect(url).toContain('off=0')
    expect(url).toContain('n=3')
    expect(url).not.toContain('empty')
    expect(url).not.toContain('none')
    expect(init.headers['Authorization']).toBe('Bearer tk')
  })

  it('有內容時送 JSON 且沒有令牌就不帶授權標頭', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 200, msg: 'OK', data: null })))
    vi.stubGlobal('fetch', fetchMock)
    await request(null, 'POST', '/x', {}, { a: 1 })
    const init = fetchMock.mock.calls[0]![1]
    expect(init.headers['Authorization']).toBeUndefined()
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(init.body)).toEqual({ a: 1 })
  })

  it('失敗時 msg 成為錯誤代碼並帶上細節', async () => {
    respond(429, { code: 429, msg: 'LOGIN_LOCKED', data: { retryAfterSeconds: '5' } })
    const err = (await request(null, 'GET', '/x').catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(429)
    expect(err.code).toBe('LOGIN_LOCKED')
    expect(err.details.retryAfterSeconds).toBe('5')
  })

  it('不是 Result 格式或不是 JSON 時用預設代碼', async () => {
    respond(200, [])
    expect(((await request(null, 'GET', '/x').catch((e) => e)) as ApiError).code).toBe('ERROR')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('boom', { status: 500 })))
    const err = (await request(null, 'GET', '/x').catch((e) => e)) as ApiError
    expect(err.code).toBe('ERROR')
    expect(err.status).toBe(500)
  })
})

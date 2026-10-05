import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, get, post } from '../http'

function mockFetch(status: number, body: unknown) {
  const fn = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), { status }),
  )
  vi.stubGlobal('fetch', fn)
  return fn
}

describe('http', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('略過空值參數', async () => {
    const fn = mockFetch(200, {})
    await get('/cards', { keyword: 'luffy', rarity: '', page: 2, color: undefined })
    expect(fn.mock.calls[0]![0]).toBe('/api/cards?keyword=luffy&page=2&lang=zh-TW')
  })

  it('一律帶語言參數', async () => {
    const fn = mockFetch(200, [])
    await get('/sets')
    expect(fn.mock.calls[0]![0]).toBe('/api/sets?lang=zh-TW')
  })

  it('失敗時拋出錯誤', async () => {
    mockFetch(404, { code: 'CARD_NOT_FOUND', status: 404 })
    const err = (await get('/cards/1').catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(404)
    expect(err.code).toBe('CARD_NOT_FOUND')
  })

  it('POST 會送出 JSON', async () => {
    const fn = mockFetch(201, { ok: true })
    await post('/orders', { a: 1 })
    const [url, init] = fn.mock.calls[0]!
    expect(url).toBe('/api/orders?lang=zh-TW')
    expect(init.method).toBe('POST')
    expect(init.headers).toEqual({ 'Content-Type': 'application/json' })
    expect(init.body).toBe('{"a":1}')
  })

  it('錯誤會帶出欄位細節', async () => {
    mockFetch(400, { code: 'VALIDATION_ERROR', status: 400, details: { 'customer.email': 'bad' } })
    const err = (await post('/orders', {}).catch((e) => e)) as ApiError
    expect(err.code).toBe('VALIDATION_ERROR')
    expect(err.details['customer.email']).toBe('bad')
  })

  it('回應不是 JSON 時用預設代碼', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('boom', { status: 500 })))
    const err = (await get('/x').catch((e) => e)) as ApiError
    expect(err.code).toBe('ERROR')
    expect(err.status).toBe(500)
  })
})

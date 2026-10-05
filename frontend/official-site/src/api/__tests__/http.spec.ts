import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, get } from '../http'

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
    expect(fn).toHaveBeenCalledWith('/api/cards?keyword=luffy&page=2&lang=zh-TW')
  })

  it('一律帶語言參數', async () => {
    const fn = mockFetch(200, [])
    await get('/sets')
    expect(fn).toHaveBeenCalledWith('/api/sets?lang=zh-TW')
  })

  it('失敗時拋出錯誤', async () => {
    mockFetch(404, { code: 'CARD_NOT_FOUND', status: 404 })
    const err = (await get('/cards/1').catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(404)
    expect(err.code).toBe('CARD_NOT_FOUND')
  })
})

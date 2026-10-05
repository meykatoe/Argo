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
    expect(fn).toHaveBeenCalledWith('/api/cards?keyword=luffy&page=2')
  })

  it('無參數不帶問號', async () => {
    const fn = mockFetch(200, [])
    await get('/sets')
    expect(fn).toHaveBeenCalledWith('/api/sets')
  })

  it('失敗時拋出錯誤', async () => {
    mockFetch(404, { message: '找不到卡片' })
    const err = (await get('/cards/1').catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(404)
    expect(err.message).toBe('找不到卡片')
  })
})

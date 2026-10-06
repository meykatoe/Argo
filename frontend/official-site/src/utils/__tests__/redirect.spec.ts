import { describe, expect, it } from 'vitest'
import { safeRedirect } from '../redirect'

describe('safeRedirect', () => {
  it('站內路徑可用', () => {
    expect(safeRedirect('/checkout')).toBe('/checkout')
    expect(safeRedirect('/cards?setId=OP-01')).toBe('/cards?setId=OP-01')
  })

  it('外部網址與怪異輸入一律回預設', () => {
    for (const v of ['https://evil.example', '//evil.example', '/\\evil', 'javascript:alert(1)', '', undefined, ['/a'], 5]) {
      expect(safeRedirect(v)).toBe('/')
    }
    expect(safeRedirect('http://x', '/account')).toBe('/account')
  })
})

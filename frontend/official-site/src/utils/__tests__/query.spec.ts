import { describe, expect, it } from 'vitest'
import { parseQuery, toQuery } from '../query'

describe('query', () => {
  it('解析預設值', () => {
    const p = parseQuery({})
    expect(p.page).toBe(1)
    expect(p.sortBy).toBe('cardSetId')
    expect(p.desc).toBe(false)
    expect(p.inStock).toBe(false)
    expect(p.category).toBeUndefined()
  })

  it('忽略不合法參數', () => {
    const p = parseQuery({ page: '-3', sortBy: 'marketPrice', category: 'x' })
    expect(p.page).toBe(1)
    expect(p.sortBy).toBe('cardSetId')
    expect(p.category).toBeUndefined()
  })

  it('解析完整條件', () => {
    const p = parseQuery({
      keyword: 'zoro',
      page: '3',
      desc: 'true',
      inStock: 'true',
      sortBy: 'salePrice',
    })
    expect(p).toMatchObject({
      keyword: 'zoro',
      page: 3,
      desc: true,
      inStock: true,
      sortBy: 'salePrice',
    })
  })

  it('輸出時略過預設值', () => {
    expect(toQuery({ keyword: '', page: 1, sortBy: 'cardSetId', desc: false })).toEqual({})
    expect(toQuery({ rarity: 'SR', page: 2, desc: true, inStock: true })).toEqual({
      rarity: 'SR',
      page: '2',
      inStock: 'true',
      desc: 'true',
    })
  })
})

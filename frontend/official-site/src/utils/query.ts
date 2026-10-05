import type { LocationQuery, LocationQueryRaw } from 'vue-router'
import type { CardCategory, CardSearchParams } from '@/types/card'

export const DEFAULT_SORT = 'cardSetId'
export const PAGE_SIZE = 24

const SORTS = ['cardSetId', 'cardName', 'salePrice']
const CATEGORIES = ['booster', 'starter', 'promo']

function first(v?: LocationQuery[string]): string {
  const s = Array.isArray(v) ? v[0] : v
  return s ?? ''
}

// 網址參數轉搜尋條件
export function parseQuery(q: LocationQuery): CardSearchParams {
  const page = Number(first(q.page))
  const sortBy = first(q.sortBy)
  const category = first(q.category)
  return {
    keyword: first(q.keyword),
    setId: first(q.setId),
    category: CATEGORIES.includes(category) ? (category as CardCategory) : undefined,
    color: first(q.color),
    rarity: first(q.rarity),
    cardType: first(q.cardType),
    page: Number.isInteger(page) && page > 0 ? page : 1,
    sortBy: SORTS.includes(sortBy) ? (sortBy as CardSearchParams['sortBy']) : DEFAULT_SORT,
    inStock: first(q.inStock) === 'true',
    desc: first(q.desc) === 'true',
  }
}

// 搜尋條件轉網址參數
export function toQuery(p: CardSearchParams): LocationQueryRaw {
  const out: LocationQueryRaw = {}
  const text = ['keyword', 'setId', 'category', 'color', 'rarity', 'cardType'] as const
  for (const key of text) {
    if (p[key]) out[key] = p[key]
  }
  if (p.page && p.page > 1) out.page = String(p.page)
  if (p.sortBy && p.sortBy !== DEFAULT_SORT) out.sortBy = p.sortBy
  if (p.inStock) out.inStock = 'true'
  if (p.desc) out.desc = 'true'
  return out
}

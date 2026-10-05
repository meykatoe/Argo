// 對應後端 API 型別

export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export interface CardSummary {
  id: number
  cardSetId: string
  cardImageId: string
  setId: string
  cardName: string
  rarity: string
  cardColor: string
  cardType: string
  cardCost: string | null
  cardPower: string | null
  imageUrl: string | null
  marketPrice: number
}

export interface CardDetail extends CardSummary {
  setName: string | null
  cardText: string | null
  counterAmount: number | null
  life: string | null
  attribute: string | null
  subTypes: string | null
}

export interface CardSet {
  setId: string
  setName: string
  category: CardCategory
}

export type CardCategory = 'booster' | 'starter' | 'promo'

export interface CardSearchParams {
  keyword?: string
  setId?: string
  category?: CardCategory
  color?: string
  rarity?: string
  cardType?: string
  page?: number
  size?: number
  sortBy?: 'cardSetId' | 'cardName' | 'marketPrice'
  desc?: boolean
}

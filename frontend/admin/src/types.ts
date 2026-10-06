export interface AdminCard {
  id: number
  cardSetId: string
  cardName: string
  rarity: string
  imageUrl: string | null
  marketPrice: number
  listPrice: number
  extraDiscount: number
  salePrice: number
  priceOverridden: boolean
  stock: number
}

export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export type StaffRole = 'ADMIN' | 'GENERAL' | 'SERVICE'

export interface Session {
  token: string
  username: string
  role: StaffRole
  expiresAt: string
}

export interface MenuNode {
  code: string
  title: string
  path: string | null
  children: MenuNode[]
}

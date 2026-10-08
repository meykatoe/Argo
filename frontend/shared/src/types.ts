// 是否類欄位一律為 0 或 1
export type Flag = 0 | 1

export type StaffRole = 'ADMIN' | 'GENERAL' | 'SERVICE' | 'OPS'

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

export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

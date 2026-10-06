export type StaffRole = 'ADMIN' | 'GENERAL' | 'SERVICE' | 'OPS'

export interface Session {
  token: string
  username: string
  role: StaffRole
  expiresAt: string
}

export type AuditAction =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILED'
  | 'LOGIN_LOCKED'
  | 'LOGOUT'
  | 'ACCESS_DENIED'
  | 'ACCOUNT_CREATED'
  | 'CARD_EXTRA_DISCOUNT_UPDATE'
  | 'AUDIT_LOG_VIEWED'

export interface AuditLog {
  id: number
  staffId: number | null
  username: string
  role: StaffRole | null
  action: AuditAction
  targetType: string | null
  targetId: string | null
  detail: Record<string, unknown> | null
  success: boolean
  ip: string | null
  userAgent: string | null
  createdAt: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

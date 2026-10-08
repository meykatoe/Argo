export type StaffRole = 'ADMIN' | 'GENERAL' | 'SERVICE' | 'OPS'

export interface Session {
  token: string
  username: string
  role: StaffRole
  expiresAt: string
}

// 是否類欄位一律為 0 或 1
export type Flag = 0 | 1

export type AuditAction =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILED'
  | 'LOGIN_LOCKED'
  | 'LOGOUT'
  | 'ACCESS_DENIED'
  | 'ACCOUNT_CREATED'
  | 'CARD_EXTRA_DISCOUNT_UPDATE'
  | 'CARD_SET_ON_SALE_UPDATE'
  | 'CARD_SET_EXTRA_DISCOUNT_UPDATE'
  | 'ORDER_SHIPPED'
  | 'ORDER_COMPLETED'
  | 'ORDER_CANCELLED'
  | 'ORDER_NOTE_UPDATED'
  | 'IP_BLOCKED'
  | 'IP_UNBLOCKED'
  | 'IP_RULE_UPDATED'
  | 'IP_ALLOWLIST_ADDED'
  | 'IP_ALLOWLIST_REMOVED'
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
  success: Flag
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

export interface MenuNode {
  code: string
  title: string
  path: string | null
  children: MenuNode[]
}

export interface IpActivity {
  ip: string
  rateLimited: number
  loginFailed: number
  blockedHits: number
  firstSeen: string
  lastSeen: string
  blocked: Flag
  blockExpiresAt: string | null
  blockReason: string | null
}

export interface IpBlock {
  ip: string
  reason: string
  blockedBy: string
  createdAt: string
  expiresAt: string | null
}

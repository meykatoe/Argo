import type { StaffRole } from '@/types'

const LABELS: Record<StaffRole, string> = {
  ADMIN: '最高權限',
  GENERAL: '一般管理員',
  SERVICE: '客服',
}

export function roleLabel(role: StaffRole): string {
  return LABELS[role] ?? role
}

// 與後端額外折扣端點的允許角色一致
export function canManageCards(role: StaffRole): boolean {
  return role === 'ADMIN' || role === 'GENERAL'
}
